package pl.catalogic.demo.s3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.catalogic.demo.s3.model.BucketDto;
import pl.catalogic.demo.s3.model.S3ObjectDto;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.endpoints.internal.Value.Str;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoncurrentVersionExpiration;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

@Service
public class S3synchro {
  private final S3AsyncClient s3Client;
  private final S3AsyncClient s3ClientTester;
  private static final long PART_SIZE = 50 * 1024 * 1024;
  private static final int MAX_CONCURRENT_OPERATIONS = 20;
  private final ExecutorService executorService;

  private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_OPERATIONS);

  public S3synchro(
      @Qualifier("s3ClientBackup") S3AsyncClient s3Client,
      @Qualifier("s3ClientTester") S3AsyncClient s3ClientTester) {
    this.s3Client = s3Client;
    this.s3ClientTester = s3ClientTester;
    this.executorService = Executors.newFixedThreadPool(10);
  }

  @SneakyThrows
  @Async
  public void transferBucketObjects(String sourceBucketName, String destinationBucketName) {
    List<S3Object> allS3Objects = getAll3SObjects(sourceBucketName);
    createBucket(destinationBucketName);

    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (S3Object object : allS3Objects) {
      if (object.key().endsWith("/")) {
        System.out.println("Skipping folder: " + object.key());
        continue;
      }

      CompletableFuture<Void> future;
      if (object.size() > PART_SIZE) {
        future = CompletableFuture.runAsync(() -> {
          try {
            multipartUploadFromS3(sourceBucketName, destinationBucketName, object.key());
          } catch (IOException e) {
            e.printStackTrace();
          }
        }, executorService);
      } else {
        future = CompletableFuture.runAsync(() -> {
          simpleUpload(sourceBucketName, destinationBucketName, object.key()).join();
        }, executorService);
      }

      futures.add(future);
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    executorService.shutdown();
  }


  public void multipartUploadFromS3(String sourceBucket, String destinationBucket, String key)
      throws IOException {

    System.out.println(key);
    // Krok 1: Pobierz obiekt jako InputStream z S3
    CompletableFuture<ResponseInputStream<GetObjectResponse>> getObjectResponseFuture =
        s3Client.getObject(
            GetObjectRequest.builder().bucket(sourceBucket).key(key).build(),
            AsyncResponseTransformer.toBlockingInputStream());

    InputStream inputStream = getObjectResponseFuture.join();

    // Krok 2: Inicjuj multipart upload na nowy bucket
    CompletableFuture<CreateMultipartUploadResponse> createResponseFuture =
        s3ClientTester.createMultipartUpload(
            CreateMultipartUploadRequest.builder().bucket(destinationBucket).key(key).build());

    String uploadId = createResponseFuture.join().uploadId();
    System.out.println("Upload initiated with uploadId: " + uploadId);

    List<CompletedPart> completedParts = new ArrayList<>();
    byte[] buffer = new byte[(int) PART_SIZE];
    int bytesRead;
    int partNumber = 1;

    // Krok 3: Odczytaj InputStream w częściach i przesyłaj
    while ((bytesRead = inputStream.read(buffer)) > 0) {
      ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);

      AsyncRequestBody requestBody = AsyncRequestBody.fromByteBuffer(byteBuffer);
      CompletableFuture<UploadPartResponse> uploadPartResponseFuture =
          s3ClientTester.uploadPart(
              UploadPartRequest.builder()
                  .bucket(destinationBucket)
                  .key(key)
                  .uploadId(uploadId)
                  .partNumber(partNumber)
                  .build(),
              requestBody);

      completedParts.add(
          CompletedPart.builder()
              .partNumber(partNumber)
              .eTag(uploadPartResponseFuture.join().eTag())
              .build());

      partNumber++;
    }

    inputStream.close();

    // Krok 4: Zakończ multipart upload
    CompletableFuture<CompleteMultipartUploadResponse> completeResponseFuture =
        s3ClientTester.completeMultipartUpload(
            CompleteMultipartUploadRequest.builder()
                .bucket(destinationBucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                .build());

    completeResponseFuture
        .whenComplete(
            (resp, err) -> {
              if (resp != null) {
                System.out.println("Multipart upload completed successfully.");
              } else {
                System.err.println("Multipart upload failed: " + err.getMessage());
                // Można anulować upload w przypadku błędu
                s3ClientTester.abortMultipartUpload(
                    AbortMultipartUploadRequest.builder()
                        .bucket(destinationBucket)
                        .key(key)
                        .uploadId(uploadId)
                        .build());
              }
            })
        .join();
  }

  public CompletableFuture<Void> simpleUpload(
      String sourceBucketName, String destinationBucketName, String objectKey) {
    // Створюємо запит на отримання об'єкта
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(sourceBucketName).key(objectKey).build();

    // Отримуємо об'єкт з S3 і копіюємо його до іншого бакета
    return s3Client
        .getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
        .thenCompose(
            objectBytes -> {
              PutObjectRequest putObjectRequest =
                  PutObjectRequest.builder().bucket(destinationBucketName).key(objectKey).build();

              // Копіюємо об'єкт в інший бакет
              return s3ClientTester
                  .putObject(
                      putObjectRequest, AsyncRequestBody.fromBytes(objectBytes.asByteArray()))
                  .thenRun(() -> System.out.println("Object copied: " + objectKey));
            })
        .exceptionally(
            ex -> {
              System.err.println("Error copying object: " + ex.getMessage());
              return null;
            });
  }

  /////////////////////////////////////////////// versioning and lifecycle

  public CompletableFuture<Void> createBucket(String bucketName) {
    return s3ClientTester
        .listBuckets(ListBucketsRequest.builder().build())
        .thenCompose(
            listBucketsResponse -> {
              boolean bucketExists =
                  listBucketsResponse.buckets().stream()
                      .anyMatch(bucket -> bucket.name().equals(bucketName));

              if (bucketExists) {
                System.out.println("Bucket with name '" + bucketName + "' already exists.");
                return CompletableFuture.completedFuture(null);
              }

              return s3ClientTester
                  .createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
                  .thenCompose(
                      createBucketResponse -> {
                        System.out.println("Bucket '" + bucketName + "' created successfully.");
                        return enableVersioning(bucketName, true)
                            .thenCompose(versioningResult -> setLifecyclePolicy(bucketName));
                      });
            })
        .exceptionally(
            ex -> {
              System.err.println("Error creating bucket: " + ex.getMessage());
              return null;
            });
  }

  private CompletableFuture<Void> enableVersioning(String bucketName, boolean isEnabled) {

    BucketVersioningStatus status = isEnabled ? BucketVersioningStatus.ENABLED : BucketVersioningStatus.SUSPENDED;

    VersioningConfiguration versioningConfiguration =
        VersioningConfiguration.builder().status(status).build();

    PutBucketVersioningRequest request =
        PutBucketVersioningRequest.builder()
            .bucket(bucketName)
            .versioningConfiguration(versioningConfiguration)
            .build();

    return s3ClientTester
        .putBucketVersioning(request)
        .thenAccept(response -> System.out.println("Versioning enabled for bucket: " + bucketName));
  }

  private CompletableFuture<Void> setLifecyclePolicy(String bucketName) {
    LifecycleRule rule =
        LifecycleRule.builder()
            .id("Delete old versions after 7 days")
            .filter(LifecycleRuleFilter.builder().build()) // using for all files
            .status(ExpirationStatus.ENABLED)
            .noncurrentVersionExpiration(
                NoncurrentVersionExpiration.builder()
                    .noncurrentDays(7) // delete old versions after 7 days
                    .build())
            .build();

    BucketLifecycleConfiguration lifecycleConfiguration =
        BucketLifecycleConfiguration.builder().rules(rule).build();

    PutBucketLifecycleConfigurationRequest request =
        PutBucketLifecycleConfigurationRequest.builder()
            .bucket(bucketName)
            .lifecycleConfiguration(lifecycleConfiguration)
            .build();

    return s3ClientTester
        .putBucketLifecycleConfiguration(request)
        .thenAccept(
            response -> System.out.println("Lifecycle policy set for bucket: " + bucketName));
  }

  public List<S3Object> getAll3SObjects(String bucketName) {
    List<S3Object> allObjects = new ArrayList<>();
    String continuationToken = null;
    do {
      ListObjectsV2Request.Builder listRequestBuilder =
          ListObjectsV2Request.builder().bucket(bucketName).maxKeys(1000);
      if (continuationToken != null) {
        listRequestBuilder.continuationToken(continuationToken);
      }

      ListObjectsV2Request listRequest = listRequestBuilder.build();

      ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest).join();

      List<S3Object> contents = listResponse.contents();
      allObjects.addAll(contents);

      continuationToken = listResponse.nextContinuationToken();
    } while (continuationToken != null);
    return allObjects;
  }


  public List<BucketDto> listBuckets() {
    return s3Client.listBuckets().join().buckets().stream().map(b -> new BucketDto(b.name(), b.creationDate().toString()))
        .toList();
  }
}

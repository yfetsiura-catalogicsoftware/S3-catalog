package pl.catalogic.demo.s3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import pl.catalogic.demo.s3.model.BucketDto;
import pl.catalogic.demo.s3.model.S3ObjectDto;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoncurrentVersionExpiration;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

@Service
public class S3Service {

  private final S3AsyncClient s3Client;
  private final S3AsyncClient s3ClientTester;
  private static  final long PART_SIZE = 5 * 1024 * 1024; // 5MB частини

  public S3Service(
      @Qualifier("s3ClientBackup") S3AsyncClient s3Client,
      @Qualifier("s3ClientTester") S3AsyncClient s3ClientTester) {
    this.s3Client = s3Client;
    this.s3ClientTester = s3ClientTester;
  }

  public CompletableFuture<List<ObjectVersion>> listObjectVersions(String bucketName) {
    ListObjectVersionsRequest listRequest =
        ListObjectVersionsRequest.builder().bucket(bucketName).build();

    return s3ClientTester
        .listObjectVersions(listRequest)
        .thenApply(ListObjectVersionsResponse::versions);
  }

  @SneakyThrows
  public List<BucketDto> listBucketsAsync() {
    CompletableFuture<ListBucketsResponse> futureResponse = s3Client.listBuckets();
    return futureResponse.get().buckets().stream()
        .map(b -> new BucketDto(b.name(), b.creationDate().toString()))
        .toList();
  }

  public CompletableFuture<List<S3ObjectDto>> getAllObjects(String bucketName) {
    ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(bucketName).build();

    return s3Client
        .listObjectsV2(listRequest)
        .thenApply(ListObjectsV2Response::contents)
        .thenApply(
            contents ->
                contents.stream()
                    .map(
                        o ->
                            new S3ObjectDto(
                                o.key(),
                                o.lastModified(),
                                o.eTag(),
                                o.size(),
                                o.storageClassAsString()))
                    .toList());
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
                        return enableVersioning(bucketName)
                            .thenCompose(versioningResult -> setLifecyclePolicy(bucketName));
                      });
            })
        .exceptionally(
            ex -> {
              System.err.println("Error creating bucket: " + ex.getMessage());
              return null;
            });
  }

  private CompletableFuture<Void> enableVersioning(String bucketName) {
    VersioningConfiguration versioningConfiguration =
        VersioningConfiguration.builder().status(BucketVersioningStatus.ENABLED).build();

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

  /////////////////////////////////////////////// upload part

  public CompletableFuture<Void> transferBucket(String sourceBucketName) {
    ListObjectsRequest listRequest = ListObjectsRequest.builder().bucket(sourceBucketName).build();

    createBucket(sourceBucketName).join(); // wait for creating

    return s3Client
        .listObjects(listRequest)
        .thenCompose(
            listResponse -> {
              List<CompletableFuture<Void>> transferFutures = new ArrayList<>();

              for (S3Object object : listResponse.contents()) {
                transferFutures.add(
                    transferObjectBetweenBucketsInMemory(
                        sourceBucketName, object.key(), object.size()));
              }

              return CompletableFuture.allOf(transferFutures.toArray(new CompletableFuture[0]));
            })
        .exceptionally(
            ex -> {
              System.err.println("Error transferring bucket: " + ex.getMessage());
              return null;
            });
  }

  private CompletableFuture<Void> transferObjectBetweenBucketsInMemory(
      String sourceBucketName, String objectKey, long objectSize) {
    if (objectKey.endsWith("/")) {
      // skip folders because they arent fisically in S3
      System.out.println("Skipping folder: " + objectKey);
      return CompletableFuture.completedFuture(null);
    }
    // upload from source bucket
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(sourceBucketName).key(objectKey).build();

    // more then 5mb
    if (objectSize > 5 * 1024 * 1024) {
      return s3Client
          .getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
          .thenCompose(
              objectBytes -> {
                return multipartUpload(sourceBucketName, objectKey, objectBytes.asByteArray());
              });
    } else {
      // less then 5mb
      return s3Client
          .getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
          .thenCompose(
              objectBytes -> {
                PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder().bucket(sourceBucketName).key(objectKey).build();

                return s3ClientTester
                    .putObject(
                        putObjectRequest, AsyncRequestBody.fromBytes(objectBytes.asByteArray()))
                    .thenRun(() -> System.out.println("Object copied: " + objectKey));
              });
    }
  }


  private CompletableFuture<Void> multipartUpload(String bucketName, String keyName, byte[] data) {

    CreateMultipartUploadRequest createRequest =
        CreateMultipartUploadRequest.builder().bucket(bucketName).key(keyName).build();

    return s3ClientTester
        .createMultipartUpload(createRequest)
        .thenCompose(
            createResponse -> {
              String uploadId = createResponse.uploadId();
              System.out.println("Multipart Upload initiated. Upload ID: " + uploadId);

              // upload file parts
              return uploadParts(bucketName, keyName, data, uploadId)
                  .thenCompose(
                      completedParts ->
                          completeMultipartUpload(bucketName, keyName, uploadId, completedParts));
            });
  }

  private CompletableFuture<List<CompletedPart>> uploadParts(
      String bucketName, String keyName, byte[] data, String uploadId) {
    List<CompletableFuture<CompletedPart>> partFutures = new ArrayList<>();
    List<CompletedPart> completedParts = new ArrayList<>();

    int partNumber = 1;
    int position = 0;

    while (position < data.length) {
      int remainingBytes = Math.min((int) PART_SIZE, data.length - position);
      byte[] partData = new byte[remainingBytes];
      System.arraycopy(data, position, partData, 0, remainingBytes);

      UploadPartRequest uploadPartRequest =
          UploadPartRequest.builder()
              .bucket(bucketName)
              .key(keyName)
              .uploadId(uploadId)
              .partNumber(partNumber)
              .build();

      int finalPartNumber = partNumber;
      CompletableFuture<CompletedPart> partFuture =
          s3ClientTester
              .uploadPart(uploadPartRequest, AsyncRequestBody.fromBytes(partData))
              .thenApply(
                  uploadPartResponse -> {
                    return CompletedPart.builder()
                        .partNumber(finalPartNumber)
                        .eTag(uploadPartResponse.eTag())
                        .build();
                  });

      partFutures.add(partFuture);
      partNumber++;
      position += remainingBytes;
    }

    // waiting for all parts upload
    return CompletableFuture.allOf(partFutures.toArray(new CompletableFuture[0]))
        .thenApply(
            v -> {
              partFutures.forEach(partFuture -> completedParts.add(partFuture.join()));
              return completedParts;
            });
  }

  private CompletableFuture<Void> completeMultipartUpload(
      String bucketName, String keyName, String uploadId, List<CompletedPart> completedParts) {
    CompleteMultipartUploadRequest completeRequest =
        CompleteMultipartUploadRequest.builder()
            .bucket(bucketName)
            .key(keyName)
            .uploadId(uploadId)
            .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
            .build();

    return s3ClientTester
        .completeMultipartUpload(completeRequest)
        .thenRun(() -> System.out.println("Multipart upload completed for object: " + keyName));
  }


}

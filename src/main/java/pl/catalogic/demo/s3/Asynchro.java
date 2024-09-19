package pl.catalogic.demo.s3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.catalogic.demo.s3.model.BucketResponse;
import pl.catalogic.demo.s3.model.S3ClientException;
import pl.catalogic.demo.s3.model.S3ObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

@Service
public class Asynchro {

  private final S3AsyncClient s3Source;
  private final S3AsyncClient s3Destination;
  private static final long PART_SIZE = 50 * 1024 * 1024;
  private static final long SIMPLE_UPLOAD_SIZE = 10 * 1024 * 1024;
  private final Semaphore simpleUploadSemaphore;
  private final Semaphore multipartUploadSemaphore;

  public Asynchro(
      @Qualifier("DataCore") S3AsyncClient s3Source,
      @Qualifier("MiniO") S3AsyncClient s3Destination) {
    this.s3Source = s3Source;
    this.s3Destination = s3Destination;
    this.simpleUploadSemaphore = new Semaphore(10);
    this.multipartUploadSemaphore = new Semaphore(4);
  }

  @Async
  public void transferBucket(String sourceBucket, String fromTo) {
    List<S3AsyncClient> clients = makeClientList(fromTo);
    List<S3ObjectRequest> files =
        getS3Objects(sourceBucket, clients.get(0)).stream()
            .map(o -> new S3ObjectRequest(o.key(), o.size()))
            .toList();
    transferFiles(clients.get(0), clients.get(1), sourceBucket, sourceBucket, files);
  }

  @SneakyThrows
  public void transferFiles(
      S3AsyncClient sourceClient,
      S3AsyncClient destinationClient,
      String sourceBucket,
      String destinationBucket,
      List<S3ObjectRequest> files) {
    for (S3ObjectRequest file : files) {
      if (file.size() > SIMPLE_UPLOAD_SIZE) {
        multipartUpload(
            sourceClient, destinationClient, sourceBucket, destinationBucket, file.key());
      } else {
        simpleUpload(sourceClient, destinationClient, sourceBucket, destinationBucket, file.key());
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  private CompletableFuture<Void> multipartUpload(
      S3AsyncClient sourceClient,
      S3AsyncClient destinationClient,
      String sourceBucket,
      String destinationBucket,
      String key) {

    return CompletableFuture.runAsync(
        () -> {
          try {
            multipartUploadSemaphore.acquire();
            InputStream inputStream = getObjectFromSource(sourceClient, sourceBucket, key);

            String uploadId = initiateMultipartUpload(destinationClient, destinationBucket, key);
            System.out.println("Upload initiated with uploadId: " + uploadId);
            List<CompletedPart> completedParts =
                uploadParts(destinationClient, inputStream, uploadId, destinationBucket, key);

            completeMultipartUpload(
                destinationClient, destinationBucket, key, uploadId, completedParts);
          } catch (Exception e) {
            throw new S3ClientException("Error during multipart upload: " + e.getMessage());
          } finally {
            multipartUploadSemaphore.release();
          }
        });
  }

  private InputStream getObjectFromSource(
      S3AsyncClient sourceClient, String sourceBucket, String key) {
    CompletableFuture<ResponseInputStream<GetObjectResponse>> getObjectResponseFuture =
        sourceClient.getObject(
            GetObjectRequest.builder().bucket(sourceBucket).key(key).build(),
            AsyncResponseTransformer.toBlockingInputStream());
    return getObjectResponseFuture.join();
  }

  private String initiateMultipartUpload(
      S3AsyncClient destinationClient, String destinationBucket, String key) {
    CompletableFuture<CreateMultipartUploadResponse> createResponseFuture =
        destinationClient.createMultipartUpload(
            CreateMultipartUploadRequest.builder().bucket(destinationBucket).key(key).build());
    return createResponseFuture.join().uploadId();
  }

  private List<CompletedPart> uploadParts(
      S3AsyncClient destinationClient,
      InputStream inputStream,
      String uploadId,
      String destinationBucket,
      String key) {

    List<CompletedPart> completedParts = new ArrayList<>();
    byte[] buffer = new byte[(int) PART_SIZE];
    int bytesRead;
    int partNumber = 1;

    try (InputStream autoCloseableInputStream = inputStream) {
      while ((bytesRead = autoCloseableInputStream.read(buffer)) > 0) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
        AsyncRequestBody requestBody = AsyncRequestBody.fromByteBuffer(byteBuffer);

        CompletableFuture<UploadPartResponse> uploadPartResponseFuture =
            destinationClient.uploadPart(
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
    } catch (IOException e) {
      throw new S3ClientException("Error reading from input stream: " + e.getMessage());
    }
    return completedParts;
  }

  private void completeMultipartUpload(
      S3AsyncClient destinationClient,
      String destinationBucket,
      String key,
      String uploadId,
      List<CompletedPart> completedParts) {

    CompletableFuture<CompleteMultipartUploadResponse> completeResponseFuture =
        destinationClient.completeMultipartUpload(
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
                destinationClient.abortMultipartUpload(
                    AbortMultipartUploadRequest.builder()
                        .bucket(destinationBucket)
                        .key(key)
                        .uploadId(uploadId)
                        .build());
              }
            })
        .join();
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  private CompletableFuture<Void> simpleUpload(
      S3AsyncClient sourceClient,
      S3AsyncClient destinationClient,
      String sourceBucketName,
      String destinationBucketName,
      String objectKey) {
    return CompletableFuture.runAsync(
        () -> {
          try {
            simpleUploadSemaphore.acquire();
            GetObjectRequest getObjectRequest =
                GetObjectRequest.builder().bucket(sourceBucketName).key(objectKey).build();

            var objectBytes =
                sourceClient.getObject(getObjectRequest, AsyncResponseTransformer.toBytes()).join();

            PutObjectRequest putObjectRequest =
                PutObjectRequest.builder().bucket(destinationBucketName).key(objectKey).build();

            destinationClient
                .putObject(putObjectRequest, AsyncRequestBody.fromBytes(objectBytes.asByteArray()))
                .join();

            System.out.println("Object copied: " + objectKey);
          } catch (InterruptedException e) {
            throw new S3ClientException("Semaphore error: " + e.getMessage());
          } finally {
            simpleUploadSemaphore.release();
          }
        });
  }

  public List<S3Object> getS3Objects(String bucketName, S3AsyncClient client) {
    List<S3Object> allObjects = new ArrayList<>();
    String continuationToken = null;
    try {
      do {
        ListObjectsV2Request.Builder listRequestBuilder =
            ListObjectsV2Request.builder().bucket(bucketName).maxKeys(1000);
        if (continuationToken != null) {
          listRequestBuilder.continuationToken(continuationToken);
        }
        ListObjectsV2Request listRequest = listRequestBuilder.build();
        ListObjectsV2Response listResponse = client.listObjectsV2(listRequest).join();

        List<S3Object> contents = listResponse.contents();

        contents.stream().filter(o -> !o.key().endsWith("/")).forEach(allObjects::add);

        continuationToken = listResponse.nextContinuationToken();
      } while (continuationToken != null);
      System.out.println(allObjects.size());
      return allObjects;
    } catch (Exception e) {
      throw new S3ClientException(e.getMessage());
    }
  }

  public List<BucketResponse> listBuckets(S3AsyncClient client) {
    try {
      return client.listBuckets().join().buckets().stream()
          .map(b -> new BucketResponse(b.name(), b.creationDate().toString()))
          .toList();
    } catch (Exception e) {
      throw new S3ClientException(e.getMessage());
    }
  }

  private void createBucket(String bucketName, S3AsyncClient client) {
    client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    System.out.println("Bucket '" + bucketName + "' created successfully.");
    enableVersioning(bucketName, client);
  }

  private boolean doesBucketExist(String bucketName, S3AsyncClient client) throws Exception {
    List<Bucket> buckets = client.listBuckets().get().buckets();
    return buckets.stream().anyMatch(bucket -> bucket.name().equals(bucketName));
  }

  private void enableVersioning(String bucketName, S3AsyncClient client) {
    VersioningConfiguration versioningConfiguration =
        VersioningConfiguration.builder().status(BucketVersioningStatus.ENABLED).build();

    PutBucketVersioningRequest request =
        PutBucketVersioningRequest.builder()
            .bucket(bucketName)
            .versioningConfiguration(versioningConfiguration)
            .build();

    client.putBucketVersioning(request);
    System.out.println("Versioning is enable for bucket " + bucketName);
  }

  private void deleteBucket(String bucketName, S3AsyncClient client) {
    try {
      if (doesBucketExist(bucketName, client)) {
        client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
        System.out.println("Bucket '" + bucketName + "' deleted successfully.");
      }
    } catch (Exception e) {
      throw new S3ClientException(e.getMessage());
    }
  }

  //////////////
  private S3AsyncClient checkClient(String client) {
    return client.equals("source") ? s3Source : s3Destination;
  }

  private List<S3AsyncClient> makeClientList(String fromTo) {
    S3AsyncClient sourceClient;
    S3AsyncClient destinationClient;
    if (fromTo.equals("tominio")) {
      sourceClient = s3Source;
      destinationClient = s3Destination;
    } else {
      sourceClient = s3Destination;
      destinationClient = s3Source;
    }
    return List.of(sourceClient, destinationClient);
  }

  ///////////// client handling
  public List<BucketResponse> getBuckets(String client) {
    S3AsyncClient chooseClient = checkClient(client);
    return listBuckets(chooseClient);
  }

  public List<S3Object> getS3Objects(String bucketName, String client) {
    S3AsyncClient client1 = checkClient(client);
    return getS3Objects(bucketName, client1);
  }
}

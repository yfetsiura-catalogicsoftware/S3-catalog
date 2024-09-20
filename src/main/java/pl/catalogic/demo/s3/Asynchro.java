package pl.catalogic.demo.s3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final long SIMPLE_UPLOAD_SIZE = 5 * 1024 * 1024;
  private final Semaphore simpleUploadSemaphore;
  private final Semaphore multipartUploadSemaphore;
  private final RetryPolicy<Object> retryPolicy;
  private static final Logger logger =
      LoggerFactory.getLogger(Asynchro.class);

  public Asynchro(
      @Qualifier("MiniPartner") S3AsyncClient s3Source,
      @Qualifier("MiniO") S3AsyncClient s3Destination) {
    this.s3Source = s3Source;
    this.s3Destination = s3Destination;
    this.simpleUploadSemaphore = new Semaphore(10);
    this.multipartUploadSemaphore = new Semaphore(4);
    this.retryPolicy = new RetryPolicy<>()
        .handle(Exception.class)
        .withBackoff(10, 60, ChronoUnit.SECONDS)
        .withMaxRetries(5);
  }

  @Async
  public void transferBucket(String sourceBucket, String fromTo) {
    var clients = makeClientList(fromTo);
    var files = getS3Objects(sourceBucket, clients.get(0)).stream()
        .map(o -> new S3ObjectRequest(o.key(), o.size()))
        .toList();

    var transferResults = new ArrayList<>();

    for (S3ObjectRequest file : files) {
      CompletableFuture<Void> result;
      if (file.size() > SIMPLE_UPLOAD_SIZE) {
        result = multipartUpload(clients.get(0), clients.get(1), sourceBucket, sourceBucket, file);
      } else {
        result = simpleUpload(clients.get(0), clients.get(1), sourceBucket, sourceBucket, file);
      }
      transferResults.add(result);
    }
    CompletableFuture.allOf(transferResults.toArray(new CompletableFuture[0]))
        .thenRun(() -> logger.info("Backup completed.Copied " + files.size() + " files"))
        .exceptionally(ex -> {
          logger.error("Backup is failed. " + ex.getMessage());
          return null;
        });
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////

  private CompletableFuture<Void> multipartUpload(
      S3AsyncClient sourceClient,
      S3AsyncClient destinationClient,
      String sourceBucket,
      String destinationBucket,
      S3ObjectRequest file) {

    return CompletableFuture.runAsync(() -> {
      try {
        multipartUploadSemaphore.acquire();

        Failsafe.with(retryPolicy).run(() -> {
          var inputStream = getObjectFromSource(sourceClient, sourceBucket, file.key());

          var uploadId = initiateMultipartUpload(destinationClient, destinationBucket, file);

          var completedParts = uploadParts(destinationClient, inputStream, uploadId, destinationBucket,
              file);

          completeMultipartUpload(destinationClient, destinationBucket, file, uploadId, completedParts);
        });
      } catch (Exception e) {
        logger.error("Error multipart upload. " + e.getMessage());
        throw new S3ClientException("Error during multipart upload: " + e.getMessage());
      } finally {
        multipartUploadSemaphore.release();
      }
    });
  }

  private InputStream getObjectFromSource(
      S3AsyncClient sourceClient, String sourceBucket, String key) {
    var getObjectResponseFuture =
        sourceClient.getObject(
            GetObjectRequest.builder().bucket(sourceBucket).key(key).build(),
            AsyncResponseTransformer.toBlockingInputStream());
    return getObjectResponseFuture.join();
  }

  private String initiateMultipartUpload(
      S3AsyncClient destinationClient, String destinationBucket, S3ObjectRequest file) {
    var createResponseFuture =
        destinationClient.createMultipartUpload(
            CreateMultipartUploadRequest.builder().bucket(destinationBucket).key(file.key()).build());
    return createResponseFuture.join().uploadId();
  }

  private List<CompletedPart> uploadParts(
      S3AsyncClient destinationClient,
      InputStream inputStream,
      String uploadId,
      String destinationBucket,
      S3ObjectRequest file) {

    List<CompletedPart> completedParts = new ArrayList<>();
    var buffer = new byte[(int) SIMPLE_UPLOAD_SIZE];
    int bytesRead;
    int partNumber = 1;

    try (InputStream autoCloseableInputStream = inputStream) {
      while ((bytesRead = autoCloseableInputStream.read(buffer)) > 0) {
        var byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
        var requestBody = AsyncRequestBody.fromByteBuffer(byteBuffer);

        var uploadPartResponseFuture =
            destinationClient.uploadPart(
                UploadPartRequest.builder()
                    .bucket(destinationBucket)
                    .key(file.key())
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build(),
                requestBody);

        completedParts.add(
            CompletedPart.builder()
                .partNumber(partNumber)
                .eTag(uploadPartResponseFuture.join().eTag())
                .build());
        logger.info("Part " + partNumber + " for file: " + file.key() + " uploaded successfully");
        partNumber++;
      }
    } catch (IOException e) {
      logger.error("Error reading from input stream: " + e.getMessage());
      throw new S3ClientException("Error reading from input stream: " + e.getMessage());
    }
    return completedParts;
  }

  private void completeMultipartUpload(
      S3AsyncClient destinationClient,
      String destinationBucket,
      S3ObjectRequest file,
      String uploadId,
      List<CompletedPart> completedParts) {

    var completeResponseFuture =
        destinationClient.completeMultipartUpload(
            CompleteMultipartUploadRequest.builder()
                .bucket(destinationBucket)
                .key(file.key())
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                .build());

    completeResponseFuture
        .whenComplete(
            (resp, err) -> {
              if (resp == null) {
                logger.error("Multipart upload failed: " + err.getMessage());
                destinationClient.abortMultipartUpload(
                    AbortMultipartUploadRequest.builder()
                        .bucket(destinationBucket)
                        .key(file.key())
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
      S3ObjectRequest file) {

    return CompletableFuture.runAsync(() -> {
      try {
        simpleUploadSemaphore.acquire();
        Failsafe.with(retryPolicy).run(() -> {
          try {
            var getObjectRequest = GetObjectRequest.builder()
                .bucket(sourceBucketName)
                .key(file.key())
                .build();

            var objectBytes = sourceClient
                .getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
                .join();

            var putObjectRequest = PutObjectRequest.builder()
                .bucket(destinationBucketName)
                .key(file.key())
                .build();

            destinationClient
                .putObject(putObjectRequest, AsyncRequestBody.fromBytes(objectBytes.asByteArray()))
                .join();

            logger.info("Object copied: " + file.key());

          } catch (Exception e) {
            logger.error("Error during simple upload for file: " + file.key() + ", reason: " + e.getMessage());
            throw new S3ClientException("Failed to upload file: " + file.key() + ", Reason: " + e.getMessage());
          }
        });

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
        var listRequest = listRequestBuilder.build();
        var listResponse = client.listObjectsV2(listRequest).join();

        var contents = listResponse.contents();

        contents.stream().filter(o -> !o.key().endsWith("/")).forEach(allObjects::add);

        continuationToken = listResponse.nextContinuationToken();
      } while (continuationToken != null);
      return allObjects;
    } catch (Exception e) {
      logger.error("Failed to list objects: " + e.getMessage());
      throw new S3ClientException(e.getMessage());
    }
  }

  public List<BucketResponse> listBuckets(S3AsyncClient client) {
    try {
      return client.listBuckets().join().buckets().stream()
          .map(b -> new BucketResponse(b.name(), b.creationDate().toString()))
          .toList();
    } catch (Exception e) {
      logger.error("Failed to list buckets: " + e.getMessage());
      throw new S3ClientException(e.getMessage());
    }
  }

  private void createBucket(String bucketName, S3AsyncClient client) {
    client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    enableVersioning(bucketName, client);
  }

  private boolean doesBucketExist(String bucketName, S3AsyncClient client) throws Exception {
    var buckets = client.listBuckets().get().buckets();
    return buckets.stream().anyMatch(bucket -> bucket.name().equals(bucketName));
  }

  private void enableVersioning(String bucketName, S3AsyncClient client) {
    var versioningConfiguration =
        VersioningConfiguration.builder().status(BucketVersioningStatus.ENABLED).build();

    var request =
        PutBucketVersioningRequest.builder()
            .bucket(bucketName)
            .versioningConfiguration(versioningConfiguration)
            .build();

    client.putBucketVersioning(request);
  }

  private void deleteBucket(String bucketName, S3AsyncClient client) {
    try {
      if (doesBucketExist(bucketName, client)) {
        client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
      }
    } catch (Exception e) {
      logger.error("Failed delete bucket: " + e.getMessage());
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
    var chooseClient = checkClient(client);
    return listBuckets(chooseClient);
  }

  public List<S3Object> getS3Objects(String bucketName, String client) {
    var client1 = checkClient(client);
    return getS3Objects(bucketName, client1);
  }
}

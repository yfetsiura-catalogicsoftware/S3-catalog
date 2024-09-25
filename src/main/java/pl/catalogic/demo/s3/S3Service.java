package pl.catalogic.demo.s3;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.catalogic.demo.s3.model.BucketCredentials;
import pl.catalogic.demo.s3.model.BucketResponse;
import pl.catalogic.demo.s3.model.S3BackupRequest;
import pl.catalogic.demo.s3.model.S3ClientException;
import pl.catalogic.demo.s3.model.S3Credentials;
import pl.catalogic.demo.s3.model.S3ObjectResponseVersion;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

@Service
public class S3Service {

  private final RetryPolicy<Object> retryPolicy;
  private static final long SIMPLE_UPLOAD_SIZE = 50 * 1024 * 1024;

  public S3Service() {
    this.retryPolicy = new RetryPolicy<>()
        .handle(Exception.class)
        .withBackoff(30, 60, ChronoUnit.SECONDS)
        .withMaxRetries(3);
  }

  private static final Logger logger =
      LoggerFactory.getLogger(S3Service.class);

  @Async
  public void backup(S3BackupRequest backupRequest) {
    List<BucketCredentials> buckets = backupRequest.buckets();

    for (BucketCredentials bucket : buckets) {
      try {
        logger.info("Starting backup for bucket: " + bucket.sourceCredentials().bucketName());
        transferBuckets(bucket);
        logger.info("Successfully completed backup for bucket: " + bucket.sourceCredentials().bucketName());
      } catch (Exception e) {
        logger.error(
            "Backup failed for bucket: " + bucket.sourceCredentials().bucketName() + ", reason: " + e.getMessage());
      }
    }

  }

  private void transferBuckets(BucketCredentials bucketCredentials) {
    S3Client sourceClient = createS3Client(bucketCredentials.sourceCredentials());
    S3Client destinationClient = createS3Client(bucketCredentials.destinationCredentials());

    if (!doesBucketExist(bucketCredentials.destinationCredentials(), destinationClient)) {
      createBucket(bucketCredentials.destinationCredentials(), destinationClient);
    }
    List<S3Object> s3Objects = getS3Objects(bucketCredentials.sourceCredentials(), sourceClient);

    for (S3Object s3Object : s3Objects) {
//      if (s3Object.size() > SIMPLE_UPLOAD_SIZE) {
//        multipartUpload(sourceClient, destinationClient, bucketCredentials, s3Object);
//      } else {
      simpleUpload(sourceClient, destinationClient, bucketCredentials, s3Object);
//      }
    }
  }

  private void multipartUpload(
      S3Client sourceClient,
      S3Client destinationClient,
      BucketCredentials credentials,
      S3Object file) {

    try {
      Failsafe.with(retryPolicy).run(() -> {
        InputStream inputStream = getObjectFromSource(sourceClient, credentials.sourceCredentials().bucketName(),
            file.key());

        String uploadId = initiateMultipartUpload(destinationClient, credentials.destinationCredentials().bucketName(),
            file.key());

        List<CompletedPart> completedParts = uploadParts(destinationClient, inputStream, uploadId,
            credentials.destinationCredentials().bucketName(), file);

        completeMultipartUpload(destinationClient, credentials.destinationCredentials().bucketName(), file, uploadId,
            completedParts);
      });
    } catch (Exception e) {
      logger.error("Error during multipart upload: " + e.getMessage());
      throw new S3ClientException("Error during multipart upload: " + e.getMessage());
    }
  }

  private InputStream getObjectFromSource(
      S3Client sourceClient, String sourceBucket, String key) {
    return sourceClient.getObject(
        GetObjectRequest.builder().bucket(sourceBucket).key(key).build());
  }

  private String initiateMultipartUpload(
      S3Client destinationClient, String destinationBucket, String key) {
    var createResponse = destinationClient.createMultipartUpload(
        CreateMultipartUploadRequest.builder()
            .bucket(destinationBucket)
            .key(key)
            .build());
    return createResponse.uploadId();
  }

  @SneakyThrows
  private List<CompletedPart> uploadParts(
      S3Client destinationClient,
      InputStream inputStream,
      String uploadId,
      String destinationBucket,
      S3Object file) {

    List<CompletedPart> completedParts = new ArrayList<>();
    byte[] buffer = new byte[(int) SIMPLE_UPLOAD_SIZE];
    int bytesRead;
    int partNumber = 1;
    long totalBytesRead = 0;

    while ((bytesRead = inputStream.read(buffer)) > 0) {
      totalBytesRead += bytesRead;

      boolean isLastPart = totalBytesRead == file.size();

      if (bytesRead < 5 * 1024 * 1024 && !isLastPart) {
        continue;
      }

      ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
      RequestBody requestBody = RequestBody.fromByteBuffer(byteBuffer);

      UploadPartResponse uploadPartResponse = destinationClient.uploadPart(
          UploadPartRequest.builder()
              .bucket(destinationBucket)
              .key(file.key())
              .uploadId(uploadId)
              .partNumber(partNumber)
              .contentLength((long) bytesRead)
              .build(),
          requestBody);

      completedParts.add(
          CompletedPart.builder()
              .partNumber(partNumber)
              .eTag(uploadPartResponse.eTag())
              .build());
      partNumber++;
    }

    System.out.println(completedParts);
    inputStream.close();
    return completedParts;
  }

  void isVersioning(S3Credentials credentials, String destinationBucket) {
    S3Client s3Client = createS3Client(credentials);
    GetBucketVersioningResponse response = s3Client.getBucketVersioning(
        GetBucketVersioningRequest.builder().bucket(destinationBucket).build());

    if (response.status() == BucketVersioningStatus.ENABLED) {
      System.out.println("Versioning is enabled");
    } else {
      System.out.println("Versioning is disabled");
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  public List<ObjectVersion> versioningList(S3Credentials credentials, String bucketName) {
    S3Client s3Client = createS3Client(credentials);

    List<ObjectVersion> allVersions = new ArrayList<>();
    String keyMarker = null;
    String versionIdMarker = null;

    do {
      ListObjectVersionsRequest request = ListObjectVersionsRequest.builder()
          .bucket(bucketName)
          .keyMarker(keyMarker)
          .versionIdMarker(versionIdMarker)
          .build();

      ListObjectVersionsResponse response = s3Client.listObjectVersions(request);

      allVersions.addAll(response.versions());

      keyMarker = response.nextKeyMarker();
      versionIdMarker = response.nextVersionIdMarker();

    } while (keyMarker != null && versionIdMarker != null);

    return allVersions;
  }

  @Async
  public void buckAll(S3BackupRequest backupRequest) {
    List<BucketCredentials> buckets = backupRequest.buckets();
    for (BucketCredentials bucket : buckets) {
      transferAllVersions(bucket);
    }
  }

  public void transferAllVersions(BucketCredentials bucketCredentials) {
    S3Client sourceClient = createS3Client(bucketCredentials.sourceCredentials());
    S3Client destinationClient = createS3Client(bucketCredentials.destinationCredentials());

    if (!doesBucketExist(bucketCredentials.destinationCredentials(), destinationClient)) {
      createBucket(bucketCredentials.destinationCredentials(), destinationClient);
    }
    List<S3ObjectResponseVersion> list = versioningList(bucketCredentials.sourceCredentials(),
        bucketCredentials.sourceCredentials().bucketName()).stream().map(
        o -> new S3ObjectResponseVersion(o.key(), o.eTag(), o.size(), o.versionId(), o.isLatest(),
            o.lastModified().toString())).toList();

    System.out.println(list);

    for (S3ObjectResponseVersion s3Object : list) {
      simpleAllVers(sourceClient, destinationClient, bucketCredentials, s3Object);
    }
  }

  private void simpleAllVers(S3Client sourceClient,
      S3Client destinationClient,
      BucketCredentials bucketCredentials,
      S3ObjectResponseVersion file) {

    var getObjectRequest = GetObjectRequest.builder()
        .bucket(bucketCredentials.sourceCredentials().bucketName())
        .key(file.key())
        .versionId(file.versionId())
        .build();

    var objectBytes = sourceClient.getObjectAsBytes(getObjectRequest).asByteArray();

    var putObjectRequest = PutObjectRequest.builder()
        .bucket(bucketCredentials.destinationCredentials().bucketName())
        .key(file.key())
        .build();

    destinationClient.putObject(putObjectRequest, RequestBody.fromBytes(objectBytes));

  }
  //////////////////

  private void completeMultipartUpload(
      S3Client destinationClient,
      String destinationBucket,
      S3Object file,
      String uploadId,
      List<CompletedPart> completedParts) {
    try {
      destinationClient.completeMultipartUpload(
          CompleteMultipartUploadRequest.builder()
              .bucket(destinationBucket)
              .key(file.key())
              .uploadId(uploadId)
              .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
              .build());
    } catch (Exception e) {
      logger.error("Multipart upload failed: " + e.getMessage());
      destinationClient.abortMultipartUpload(
          AbortMultipartUploadRequest.builder()
              .bucket(destinationBucket)
              .key(file.key())
              .uploadId(uploadId)
              .build());
      throw new S3ClientException(
          "Failed to complete multipart upload for file: " + file.key() + ", Reason: " + e.getMessage());
    }
  }


  private void simpleUpload(
      S3Client sourceClient,
      S3Client destinationClient,
      BucketCredentials bucketCredentials,
      S3Object file) {

    Failsafe.with(retryPolicy).run(() -> {
      try {
        var getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketCredentials.sourceCredentials().bucketName())
            .key(file.key())
            .build();

        var objectBytes = sourceClient.getObjectAsBytes(getObjectRequest).asByteArray();

        var putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketCredentials.destinationCredentials().bucketName())
            .key(file.key())
            .build();

        destinationClient.putObject(putObjectRequest, RequestBody.fromBytes(objectBytes));
      } catch (Exception e) {
        logger.error("Error during simple upload for file: " + file.key() + ", reason: " + e.getMessage());
        throw new S3ClientException("Failed to upload file: " + file.key() + ", Reason: " + e.getMessage());
      }
    });
  }


  public List<S3Object> getS3Objects(S3Credentials credentials, S3Client client) {
    List<S3Object> allObjects = new ArrayList<>();
    String continuationToken = null;

    try {
      do {
        ListObjectsV2Request.Builder listRequestBuilder =
            ListObjectsV2Request.builder().bucket(credentials.bucketName()).maxKeys(1000);
        if (continuationToken != null) {
          listRequestBuilder.continuationToken(continuationToken);
        }
        var listRequest = listRequestBuilder.build();
        var listResponse = client.listObjectsV2(listRequest);
        var contents = listResponse.contents();

        contents.stream()
            .filter(o -> !o.key().endsWith("/"))
            .forEach(allObjects::add);

        continuationToken = listResponse.nextContinuationToken();
      } while (continuationToken != null);
      return allObjects;
    } catch (Exception e) {
      logger.error("Failed to list objects: " + e.getMessage());
      throw new S3ClientException("Failed to list objects: " + e.getMessage());
    }
  }

  public List<BucketResponse> listBuckets(S3Credentials credentials) {
    try (var s3Client = createS3Client(credentials)) {
      return s3Client.listBuckets().buckets().stream()
          .map(b -> new BucketResponse(b.name()))
          .toList();
    } catch (Exception e) {
      logger.error("Failed to list buckets due to unexpected error: {}", e.getMessage());
      throw new S3ClientException("Failed to list buckets due to unexpected error: " + e.getMessage());
    }
  }

  private void createBucket(S3Credentials credentials, S3Client client) {
    client.createBucket(CreateBucketRequest.builder().bucket(credentials.bucketName()).build());
    enableVersioning(credentials.bucketName(), client);
  }

  private boolean doesBucketExist(S3Credentials credentials, S3Client client) {
    var buckets = client.listBuckets().buckets();
    return buckets.stream().anyMatch(bucket -> bucket.name().equals(credentials.bucketName()));
  }

  private void enableVersioning(String bucketName, S3Client client) {
    var versioningConfiguration =
        VersioningConfiguration.builder().status(BucketVersioningStatus.ENABLED).build();
    var request =
        PutBucketVersioningRequest.builder()
            .bucket(bucketName)
            .versioningConfiguration(versioningConfiguration)
            .build();
    client.putBucketVersioning(request);
  }


  public S3Client createS3Client(S3Credentials credentials) {
    return S3Client.builder()
        .endpointOverride(URI.create(credentials.s3accessEndpoint()))
        .region(Region.of(credentials.region()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(credentials.accessKey(), credentials.secretKey())))
        .forcePathStyle(true)
        .overrideConfiguration(clientOverrides -> clientOverrides.apiCallTimeout(Duration.ofMinutes(2))
            .apiCallAttemptTimeout(Duration.ofSeconds(30)))
        .build();
  }
}

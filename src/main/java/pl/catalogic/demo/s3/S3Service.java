package pl.catalogic.demo.s3;

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
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Destination;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoncurrentVersionExpiration;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ReplicationConfiguration;
import software.amazon.awssdk.services.s3.model.ReplicationRule;
import software.amazon.awssdk.services.s3.model.ReplicationRuleStatus;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

@Service
public class S3Service {

  private final S3AsyncClient s3Client;
  private final S3AsyncClient s3ClientTester;

  public S3Service(
      @Qualifier("s3ClientBackup") S3AsyncClient s3Client,
      @Qualifier("s3ClientTester") S3AsyncClient s3ClientTester) {
    this.s3Client = s3Client;
    this.s3ClientTester = s3ClientTester;
  }

  public CompletableFuture<Void> createBucketTester(String bucketName) {
    // Asynchronously list buckets
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

              // Create bucket asynchronously
              return s3ClientTester
                  .createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
                  .thenCompose(
                      createBucketResponse -> {
                        System.out.println("Bucket '" + bucketName + "' created successfully.");

                        // Enable versioning and set lifecycle policy
                        return enableVersioning(bucketName)
                            .thenCompose(versioningResult -> setLifecyclePolicy(bucketName));
                      });
            });
  }

  public void enableReplication(String sourceBucketName, String destinationBucketArn) {
    String roleArn = "arn:aws:iam::123456789012:role/policy";
    ReplicationConfiguration replicationConfiguration =
        ReplicationConfiguration.builder()
            .role(roleArn)
            .rules(
                ReplicationRule.builder()
                    .status(ReplicationRuleStatus.ENABLED)
                    .priority(1)
                    .destination(
                        Destination.builder()
                            .bucket(destinationBucketArn)
                            .storageClass(StorageClass.STANDARD)
                            .build())
                    .build())
            .build();

    PutBucketReplicationRequest request =
        PutBucketReplicationRequest.builder()
            .bucket(sourceBucketName)
            .replicationConfiguration(replicationConfiguration)
            .build();

    s3Client.putBucketReplication(request);
  }

  public CompletableFuture<Void> transferBucket(String sourceBucketName) {
    // Отримуємо всі об'єкти з бакету
    ListObjectsV2Request listRequest =
        ListObjectsV2Request.builder().bucket(sourceBucketName).build();

    // Створюємо бакет, якщо його не існує
    createBucketTester(sourceBucketName);

    return s3Client
        .listObjectsV2(listRequest)
        .thenCompose(
            listResponse -> {
              CompletableFuture<Void> allTransfers = CompletableFuture.completedFuture(null);

              // Копіюємо кожний об'єкт асинхронно
              for (S3Object object : listResponse.contents()) {
                allTransfers =
                    allTransfers.thenCombine(
                        transferObjectBetweenBucketsInMemory(sourceBucketName, object.key()),
                        (a, b) -> null);
              }

              return allTransfers;
            });
  }

  private CompletableFuture<Void> transferObjectBetweenBucketsInMemory(
      String sourceBucketName, String objectKey) {
    // Завантажуємо об'єкт з вихідного бакету
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(sourceBucketName).key(objectKey).build();

    return s3Client
        .getObject(getObjectRequest, AsyncResponseTransformer.toBytes())
        .thenCompose(
            objectBytes -> {
              // Завантажуємо об'єкт у цільовий бакет
              PutObjectRequest putObjectRequest =
                  PutObjectRequest.builder().bucket(sourceBucketName).key(objectKey).build();

              return s3ClientTester
                  .putObject(
                      putObjectRequest, AsyncRequestBody.fromBytes(objectBytes.asByteArray()))
                  .thenRun(() -> System.out.println("Object copied: " + objectKey));
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
            .filter(LifecycleRuleFilter.builder().build()) // застосовується до всіх файлів
            .status(ExpirationStatus.ENABLED)
            .noncurrentVersionExpiration(
                NoncurrentVersionExpiration.builder()
                    .noncurrentDays(7) // Видалити старі версії через 7 днів
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

  public CompletableFuture<List<ObjectVersion>> listObjectVersions(String bucketName) {
    ListObjectVersionsRequest listRequest =
        ListObjectVersionsRequest.builder().bucket(bucketName).build();

    return s3ClientTester
        .listObjectVersions(listRequest)
        .thenApply(ListObjectVersionsResponse::versions);
  }

  public CompletableFuture<List<S3ObjectDto>> getAllObjects(String bucketName) {
    ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(bucketName).build();

    // Асинхронний запит для отримання списку об'єктів
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

  @SneakyThrows
  public List<BucketDto> listBucketsAsync() {
    CompletableFuture<ListBucketsResponse> futureResponse = s3Client.listBuckets();
    return futureResponse.get().buckets().stream()
        .map(b -> new BucketDto(b.name(), b.creationDate().toString()))
        .toList();
  }
}

package pl.catalogic.demo.s3.v2;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshot;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshotRepository;
import pl.catalogic.demo.s3.v2.model.S3BucketPurpose;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

@Component
@AllArgsConstructor
public class S3_v2_service {

  private final S3Client s3Client;
  private final TestAggregator testAggregator;
  private final NonVersioningTransferAggregator nonVersioningTransferAggregator;
  private final VersioningTransferAggregator versioningTransferAggregator;
  private static final String access = "NgiuzPiqBeW6s0Z2Cvyt";
  private static final String secret = "Cyif4IqnUfaz40Pezv3r5YqpZGOIgH01UC3FCZ7f";
  private static final String end = "http://172.26.0.137:9004";
  private final ObjectVersionSnapshotRepository repository;

  @SneakyThrows
  public void getAllFroms() {
    var from = Date.from(Instant.parse("2025-07-15T09:00:00.00Z"));
    var to = Date.from(Instant.parse("2025-07-15T12:30:00.00Z"));

    //    var list = versioningTransferAggregator.toDeleteBeforeTransfer(
    //        UUID.fromString("00000000-0000-0000-0000-000000000000"),from,to,
    // "bucket","sourceEnd");
    //    var list = nonVersioningTransferAggregator.toTransfer(
    //        UUID.fromString("00000000-0000-0000-0000-000000000000"), from, to, "bucket",
    // "sourceEnd");
        var list = nonVersioningTransferAggregator.delete(
            UUID.fromString("00000000-0000-0000-0000-000000000000"), "milion", "endpoint");
//    var list =
//        versioningTransferAggregator.toTransfer(
//            UUID.fromString("00000000-0000-0000-0000-000000000000"),
//            from,
//            to,
//            "bucket",
//            "sourceEnd");
//    var list =
//        versioningTransferAggregator.toDeleteBeforeTransfer(
//            UUID.fromString("00000000-0000-0000-0000-000000000000"),
//            from,
//            to,
//            "bucket",
//            "sourceEnd");
//
//    var list =
//        versioningTransferAggregator.toDeleteFilesThatDontExistOnTheSource(
//            UUID.fromString("00000000-0000-0000-0000-000000000000"),
//            "bucket",
//            "sourceEnd");

    System.out.println("---------files------------------");
    list.stream().forEach(System.out::println);
    System.out.println("---------------------------");
  }

  public void generate() {
    List<ObjectVersionSnapshot> toSav = new ArrayList<>();
    for(int i=0; i<250_000;i++){
      toSav.add(new ObjectVersionSnapshot(
          "",
          i+"_object",
          Instant.parse("2023-01-01T00:00:00.00Z"),
          S3BucketPurpose.SOURCE,
          1024,
          UUID.fromString("00000000-0000-0000-0000-000000000000"),
          "endpoint",
          "milion"
      ));
    }
    repository.saveAll(toSav);
    List<ObjectVersionSnapshot> toSaveDest = new ArrayList<>();
    for(int j=0; j<250_000;j++){
      toSaveDest.add(new ObjectVersionSnapshot(
          "",
          j+"_object",
          Instant.parse("2023-01-01T23:00:00.00Z"),
          S3BucketPurpose.DESTINATION,
          1024,
          UUID.fromString("00000000-0000-0000-0000-000000000000"),
          "endpoint",
          "milion"
      ));
    }
    repository.saveAll(toSaveDest);
  }

  /**
   * Отримує список всіх доступних бакетів
   */
  public CompletableFuture<List<Bucket>> getAllBuckets(S3AsyncClient client) {
    return client.listBuckets()
        .thenApply(ListBucketsResponse::buckets)
        .thenApply(buckets -> {
          System.out.println("---------buckets------------------");
          buckets.forEach(bucket -> 
              System.out.println("Bucket: " + bucket.name() + ", Created: " + bucket.creationDate()));
          System.out.println("Total buckets: " + buckets.size());
          System.out.println("---------------------------");
          return buckets;
        })
        .exceptionally(throwable -> {
          System.err.println("Error getting buckets: " + throwable.getMessage());
          throw new RuntimeException(throwable);
        });
  }

  /**
   * Рахує кількість файлів у вказаному бакеті
   */
  public CompletableFuture<Integer> countObjectsInBucket(S3AsyncClient client, String bucketName) {
    return countObjectsRecursively(client, bucketName, null, 0)
        .thenApply(count -> {
          System.out.println("---------object count------------------");
          System.out.println("Bucket: " + bucketName + ", Files count: " + count);
          System.out.println("---------------------------");
          return count;
        })
        .exceptionally(throwable -> {
          System.err.println("Error counting files in bucket " + bucketName + ": " + throwable.getMessage());
          throw new RuntimeException(throwable);
        });
  }

  private CompletableFuture<Integer> countObjectsRecursively(
      S3AsyncClient client, String bucketName, String continuationToken, int currentCount) {
    
    var requestBuilder = ListObjectsV2Request.builder()
        .bucket(bucketName)
        .maxKeys(1000);
    
    if (continuationToken != null) {
      requestBuilder.continuationToken(continuationToken);
    }
    
    return client.listObjectsV2(requestBuilder.build())
        .thenCompose(response -> {
          var newCount = currentCount + response.keyCount();
          
          if (response.isTruncated()) {
            return countObjectsRecursively(client, bucketName, 
                response.nextContinuationToken(), newCount);
          } else {
            return CompletableFuture.completedFuture(newCount);
          }
        });
  }
}

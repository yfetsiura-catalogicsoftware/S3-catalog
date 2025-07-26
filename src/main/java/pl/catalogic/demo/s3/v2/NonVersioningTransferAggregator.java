package pl.catalogic.demo.s3.v2;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshot;
import pl.catalogic.demo.s3.v2.model.S3BucketPurpose;

@Component
public class NonVersioningTransferAggregator {
  public static final int S3_BATCH_SIZE = 1000;
  private final MongoTemplate catalogMongoTemplate;

  public NonVersioningTransferAggregator(MongoTemplate catalogMongoTemplate) {
    this.catalogMongoTemplate = catalogMongoTemplate;
  }

  public CloseableIterator<ObjectVersionSnapshot> toTransfer(
      String bucketName, String endpoint, UUID jobDefinitionId, Date fromDate, Date toDate) {
    var startTime = Instant.now();
    System.out.println("Start toTransfer " + startTime);
    var aggregation =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("s3BucketPurpose")
                    .is(S3BucketPurpose.SOURCE)
                    .and("jobDefinitionGuid")
                    .is(jobDefinitionId)
                    .and("bucket")
                    .is(bucketName)
                    .and("sourceEndpoint")
                    .is(endpoint)
                    .and("lastModified")
                    .gt(fromDate)
                    .lte(toDate)),
            Aggregation.sort(Sort.Direction.ASC, "_id"));
    var stream = stream(aggregation);
    var duration = Duration.between(startTime, Instant.now());
    System.out.println("Completed transfer aggregation in " + duration.toSeconds() + " s");
    return stream;
  }


  private CloseableIterator<ObjectVersionSnapshot> stream(Aggregation agg) {
    return catalogMongoTemplate.aggregateStream(
        agg.withOptions(writeableToDiskWithCursor()),
        "object_version",
        ObjectVersionSnapshot.class);
  }

  private static AggregationOptions writeableToDiskWithCursor() {
    return AggregationOptions.builder().allowDiskUse(true).cursorBatchSize(S3_BATCH_SIZE).build();
  }

  public CloseableIterator<ObjectVersionSnapshot> findMissingSourceKeys(
      UUID jobDefinitionGuid, String bucket, String sourceEndpoint) {
    var startTime = Instant.now();
    System.out.println("Start toCleanup " + startTime);
    var agg =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("s3BucketPurpose")
                    .is(S3BucketPurpose.DESTINATION)
                    .and("jobDefinitionGuid")
                    .is(jobDefinitionGuid)
                    .and("bucket")
                    .is(bucket)
                    .and("sourceEndpoint")
                    .is(sourceEndpoint)),
            LookupOperation.newLookup()
                .from("object_version")
                .localField("key")
                .foreignField("key")
                .as("src"),
            Aggregation.match(
                Criteria.where("src")
                    .not()
                    .elemMatch(
                        Criteria.where("s3BucketPurpose")
                            .is(S3BucketPurpose.SOURCE)
                            .and("bucket")
                            .is(bucket)
                            .and("sourceEndpoint")
                            .is(sourceEndpoint)
                            .and("jobDefinitionGuid")
                            .is(jobDefinitionGuid))));
    var stream = stream(agg);
    var duration = Duration.between(startTime, Instant.now());
    System.out.println("Completed transfer aggregation in " + duration.toSeconds() + " s");
    return stream;
  }

  public List<ObjectVersionSnapshot> delete(
      UUID jobDefinitionId, String bucket, String sourceEndpoint) {
    var files = new ArrayList<ObjectVersionSnapshot>();
    try (var iterator = findMissingSourceKeys(jobDefinitionId, bucket, sourceEndpoint)) {
      while (iterator.hasNext()) {
        files.add(iterator.next());
      }
    }
    return files;
  }

  public List<ObjectVersionSnapshot> transfer(
      UUID jobDefinitionId, String bucket, String sourceEndpoint, Date fromDate, Date toDate) {
    var files = new ArrayList<ObjectVersionSnapshot>();
    try (var iterator = toTransfer(bucket, sourceEndpoint, jobDefinitionId, fromDate, toDate)) {
      while (iterator.hasNext()) {
        files.add(iterator.next());
      }
    }
    return files;
  }
}

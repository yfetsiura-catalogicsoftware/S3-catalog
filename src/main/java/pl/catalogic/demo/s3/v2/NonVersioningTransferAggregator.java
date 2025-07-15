package pl.catalogic.demo.s3.v2;

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
  public static final int MAX_VERSIONS_TO_KEEP = 2;

  private final MongoTemplate catalogMongoTemplate;

  public NonVersioningTransferAggregator(MongoTemplate catalogMongoTemplate) {
    this.catalogMongoTemplate = catalogMongoTemplate;
  }

  public CloseableIterator<ObjectVersionSnapshot> toTransfer(
      UUID jobDefinitionId, Date fromDate, Date toDate, String bucketName, String sourceEndpoint) {
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
                    .is(sourceEndpoint)
                    .and("lastModified")
                    .gt(fromDate)
                    .lte(toDate)),
            Aggregation.sort(Sort.Direction.ASC, "_id"));
    return stream(aggregation);
  }

  public CloseableIterator<ObjectVersionSnapshot> toDelete(
      UUID jobId, String bucketName, String sourceEndpoint) {
    var agg =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("s3BucketPurpose")
                    .is(S3BucketPurpose.DESTINATION)
                    .and("jobDefinitionGuid")
                    .is(jobId)
                    .and("bucket")
                    .is(bucketName)
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
                            .is(bucketName)
                            .and("sourceEndpoint")
                            .is(sourceEndpoint)
                            .and("jobDefinitionGuid")
                            .is(jobId))));
    return stream(agg);
  }


  private CloseableIterator<ObjectVersionSnapshot> stream(Aggregation agg) {
    return CloseableIteratorImpl.toCloseableIterator(
        catalogMongoTemplate.aggregateStream(
            agg.withOptions(writeableToDiskWithCursor()),
            "object_version",
            ObjectVersionSnapshot.class));
  }

  private static AggregationOptions writeableToDiskWithCursor() {
    return AggregationOptions.builder().allowDiskUse(true).cursorBatchSize(S3_BATCH_SIZE).build();
  }

  public List<ObjectVersionSnapshot> getAllFromRange(
      UUID jobDefinitionId, Date fromDate, Date toDate, String bucket, String sourceEndpoint) {
    var files = new ArrayList<ObjectVersionSnapshot>();
    try (var iterator = toTransfer(jobDefinitionId, fromDate, toDate, bucket, sourceEndpoint)) {
      while (iterator.hasNext()) {
        files.add(iterator.next());
      }
    }
    return files;
  }

  public List<ObjectVersionSnapshot> delete(
      UUID jobDefinitionId, String bucket, String sourceEndpoint) {
    var files = new ArrayList<ObjectVersionSnapshot>();
    try (var iterator = toDelete(jobDefinitionId, bucket, sourceEndpoint)) {
      while (iterator.hasNext()) {
        files.add(iterator.next());
      }
    }
    return files;
  }
}

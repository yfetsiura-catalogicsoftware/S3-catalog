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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.util.CloseableIterator;
@Component
public class NonVersioningTransferAggregator {
  public static final int S3_BATCH_SIZE = 1000;

  private final MongoTemplate catalogMongoTemplate;

  public NonVersioningTransferAggregator(MongoTemplate catalogMongoTemplate) {
    this.catalogMongoTemplate = catalogMongoTemplate;
  }

  public CloseableIterator<ObjectVersionSnapshot> toTransfer(
      UUID jobDefinitionId, Date fromDate, Date toDate, String bucket) {
    var aggregation = nonVersioningAggregationWithRange(jobDefinitionId, fromDate, toDate, bucket);
    return stream(aggregation);
  }

  public CloseableIterator<ObjectVersionSnapshot> toDelete(UUID jobId, String bucketName) {
    var agg =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("s3BucketPurpose")
                    .is(S3BucketPurpose.DESTINATION)
                    .and("jobDefinitionGuid")
                    .is(jobId.toString())
                    .and("bucket")
                    .is(bucketName)),
            LookupOperation.newLookup()
                .from("object_version")
                .localField("key")
                .foreignField("key")
                .pipeline(
                    Aggregation.match(
                        Criteria.where("s3BucketPurpose").is(S3BucketPurpose.SOURCE)
                            .and("bucket").is(bucketName)
                            .and("jobDefinitionGuid").is(jobId.toString())
                    )
                )
                .as("src"),
            Aggregation.match(Criteria.where("src").size(0)));
    return stream(agg);
  }

  public List<ObjectVersionSnapshot> deleteAllFromRange(UUID jobDefinitionId, String bucket) {
    var files = new ArrayList<ObjectVersionSnapshot>();
    try (var iterator = toDelete(jobDefinitionId, bucket)) {
      while (iterator.hasNext()) {
        files.add(iterator.next());
      }
    }
    return files;
  }

  // Хелпер для тестування з часовими рамками
  public List<ObjectVersionSnapshot> getAllFromRange(
      UUID jobDefinitionId, Date fromDate, Date toDate, String bucket) {
    var files = new ArrayList<ObjectVersionSnapshot>();
    try (var iterator = toTransfer(jobDefinitionId, fromDate, toDate, bucket)) {
      while (iterator.hasNext()) {
        files.add(iterator.next());
      }
    }
    return files;
  }

  private Aggregation nonVersioningAggregationWithRange(
      UUID jobDefinitionId, Date fromDate, Date toDate, String bucket) {
    return Aggregation.newAggregation(
        Aggregation.match(
            Criteria.where("s3BucketPurpose")
                .is(S3BucketPurpose.SOURCE)
                .and("jobDefinitionGuid")
                .is(jobDefinitionId.toString())
                .and("bucket")
                .is(bucket)
                .and("lastModified")
                .gt(fromDate)
                .lte(toDate)),
        Aggregation.sort(Sort.Direction.ASC, "_id"));
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
}

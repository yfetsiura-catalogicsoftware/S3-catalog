package pl.catalogic.demo.s3.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
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

  public CloseableIterator<ObjectVersionSnapshot> findMissingSourceKeys(
      UUID jobDefinitionGuid, String bucket, String sourceEndpoint) {
    var destMatch =
        Aggregation.match(
            Criteria.where("jobDefinitionGuid")
                .is(jobDefinitionGuid)
                .and("bucket")
                .is(bucket)
                .and("sourceEndpoint")
                .is(sourceEndpoint)
                .and("s3BucketPurpose")
                .is(S3BucketPurpose.DESTINATION));
    var lookupSource = Aggregation.lookup("object_version", "key", "key", "src");
    var missingSrc =
        Aggregation.match(Criteria.where("src.s3BucketPurpose").ne(S3BucketPurpose.SOURCE.name()));
    var agg = Aggregation.newAggregation(destMatch, lookupSource, missingSrc);
    return stream(agg);
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
}

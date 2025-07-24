package pl.catalogic.demo.s3.v2;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.springframework.data.domain.Sort;
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
    

    public CloseableIterator<ObjectVersionSnapshot> findMissingSourceKeys(UUID jobDefinitionGuid, String bucket, String sourceEndpoint) {
        var matchOperation = Aggregation.match(
                Criteria.where("jobDefinitionGuid").is(jobDefinitionGuid)
                        .and("bucket").is(bucket)
                        .and("sourceEndpoint").is(sourceEndpoint));

        var groupOperation = Aggregation.group("key")
                .addToSet("s3BucketPurpose").as("purposes");

        var matchMissingOperation = Aggregation.match(
                new Criteria().andOperator(
                        Criteria.where("purposes").size(1),
                        Criteria.where("purposes").in(S3BucketPurpose.DESTINATION)
                ));

        var lookupOperation = Aggregation.lookup()
                .from("object_version")
                .localField("_id")
                .foreignField("key")
                .as("snapshots");

        var matchDestinationOperation = Aggregation.match(
                Criteria.where("snapshots").elemMatch(
                        Criteria.where("s3BucketPurpose").is(S3BucketPurpose.DESTINATION)
                                .and("jobDefinitionGuid").is(jobDefinitionGuid)
                                .and("bucket").is(bucket)
                                .and("sourceEndpoint").is(sourceEndpoint)
                ));

        var unwindOperation = Aggregation.unwind("snapshots");

        var replaceRootOperation = Aggregation.replaceRoot("snapshots");

        var aggregation = Aggregation.newAggregation(
                matchOperation,
                groupOperation,
                matchMissingOperation,
                lookupOperation,
                matchDestinationOperation,
                unwindOperation,
                replaceRootOperation
        );

        return stream(aggregation);
    }

    private CloseableIterator<ObjectVersionSnapshot> stream(Aggregation agg) {
        return CloseableIteratorImpl.toCloseableIterator(
                catalogMongoTemplate.aggregateStream(
                        agg.withOptions(writeableToDiskWithCursor()),
                        "object_version",
                        ObjectVersionSnapshot.class));
    }

    private static AggregationOptions writeableToDiskWithCursor() {
        return AggregationOptions.builder()
                .allowDiskUse(true)
                .cursorBatchSize(S3_BATCH_SIZE)
                .build();
    }

    public List<ObjectVersionSnapshot> delete(UUID jobDefinitionId, String bucket, String sourceEndpoint) {
        var files = new ArrayList<ObjectVersionSnapshot>();
        try (var iterator = findMissingSourceKeys(jobDefinitionId, bucket, sourceEndpoint)) {
            while (iterator.hasNext()) {
                files.add(iterator.next());
            }
        }
        return files;
    }
}

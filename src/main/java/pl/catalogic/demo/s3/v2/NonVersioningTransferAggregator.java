package pl.catalogic.demo.s3.v2;

import java.util.*;
import java.util.UUID;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshot;
import org.springframework.data.mongodb.core.MongoTemplate;

@Component
public class NonVersioningTransferAggregator {
  public static final int S3_BATCH_SIZE = 1000;
  private final MongoTemplate mongoTemplate;
  private static final Logger log = LoggerFactory.getLogger(NonVersioningTransferAggregator.class);

  public NonVersioningTransferAggregator(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Витягує DESTINATION-об'єкти, для яких НЕ існує SOURCE-копії по key+bucket+sourceEndpoint+jobDefinitionGuid
   */
  public CloseableIterator<ObjectVersionSnapshot> toDelete(
      UUID jobId, String bucketName, String sourceEndpoint
  ) {
    var pipeline = List.of(
        new Document("$match", new Document()
            .append("s3BucketPurpose", "DESTINATION")
            .append("jobDefinitionGuid", jobId)
            .append("bucket", bucketName)
            .append("sourceEndpoint", sourceEndpoint)
        ),
        new Document("$lookup", new Document()
            .append("from", "object_version")
            .append("let", new Document()
                .append("key", "$key")
                .append("bucket", "$bucket")
                .append("sourceEndpoint", "$sourceEndpoint")
                .append("jobDefinitionGuid", "$jobDefinitionGuid")
            )
            .append("pipeline", List.of(
                new Document("$match", new Document("$expr",
                    new Document("$and", List.of(
                        new Document("$eq", List.of("$key", "$$key")),
                        new Document("$eq", List.of("$s3BucketPurpose", "SOURCE")),
                        new Document("$eq", List.of("$bucket", "$$bucket")),
                        new Document("$eq", List.of("$sourceEndpoint", "$$sourceEndpoint")),
                        new Document("$eq", List.of("$jobDefinitionGuid", "$$jobDefinitionGuid"))
                    ))
                )),
                new Document("$limit", 1)
            ))
            .append("as", "src")
        ),
        new Document("$match", new Document("src", new Document("$size", 0))),
        new Document("$sort", new Document("_id", 1))
    );

    var cursor = mongoTemplate
        .getCollection("object_version")
        .aggregate(pipeline)
        .allowDiskUse(true)
        .batchSize(S3_BATCH_SIZE);

    return CloseableIteratorImpl.toCloseableIterator(
        cursor.iterator(),
        d -> mongoTemplate.getConverter().read(ObjectVersionSnapshot.class, d)
    );
  }

  public List<ObjectVersionSnapshot> delete(UUID jobDefinitionId, String bucket, String sourceEndpoint) {
    var files = new ArrayList<ObjectVersionSnapshot>();
    try (var iterator = toDelete(jobDefinitionId, bucket, sourceEndpoint)) {
      while (iterator.hasNext()) {
        files.add(iterator.next());
      }
    }
    return files;
  }
}

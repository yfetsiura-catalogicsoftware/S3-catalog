package pl.catalogic.demo.s3.v2;

import com.mongodb.client.MongoCursor;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.Slice;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshot;
import pl.catalogic.demo.s3.v2.model.S3BucketPurpose;

@Component
public class VersioningTransferAggregator {
  public static final int S3_BATCH_SIZE = 1000;
  public static final int MAX_VERSIONS_TO_KEEP = 2;

  private final MongoTemplate catalogMongoTemplate;

  public VersioningTransferAggregator(MongoTemplate catalogMongoTemplate) {
    this.catalogMongoTemplate = catalogMongoTemplate;
  }

  public CloseableIterator<ObjectVersionSnapshot> toTransfer(
      UUID jobDefinitionId, Date fromDate, Date toDate, String bucket, String sourceEndpoint) {
    var aggregation =
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("s3BucketPurpose")
                    .is(S3BucketPurpose.SOURCE)
                    .and("jobDefinitionGuid")
                    .is(jobDefinitionId)
                    .and("bucket")
                    .is(bucket)
                    .and("sourceEndpoint")
                    .is(sourceEndpoint)
                    .and("lastModified")
                    .gt(fromDate)
                    .lte(toDate)),
            Aggregation.sort(
                Sort.by(Sort.Direction.ASC, "key")
                    .and(Sort.by(Sort.Direction.DESC, "lastModified"))),
            Aggregation.group("key", "sourceEndpoint").push("$$ROOT").as("documents"),
            Aggregation.project()
                .and(Slice.sliceArrayOf("documents").itemCount(MAX_VERSIONS_TO_KEEP))
                .as("limitedDocs"),
            Aggregation.unwind("limitedDocs"),
            Aggregation.replaceRoot("limitedDocs"),
            Aggregation.sort(Sort.Direction.ASC, "key", "lastModified"));
    return stream(aggregation);
  }

  public CloseableIterator<ObjectVersionSnapshot> toDeleteBeforeTransfer(
      UUID jobDefinitionId, Date fromDate, Date toDate, String bucket, String sourceEndpoint) {
    var pipeline =
        List.of(
            new Document(
                "$match",
                new Document()
                    .append("s3BucketPurpose", "SOURCE")
                    .append("jobDefinitionGuid", jobDefinitionId)
                    .append("bucket", bucket)
                    .append("sourceEndpoint", sourceEndpoint)
                    .append(
                        "lastModified",
                        new Document().append("$gt", fromDate).append("$lte", toDate))),
            // 2. Групуємо по key та рахуємо ФАКТИЧНУ кількість SOURCE файлів для кожного ключа
            new Document(
                "$group",
                new Document()
                    .append("_id", "$key")
                    .append("actualSourceCount", new Document("$sum", 1))),
            // 3. Lookup всіх DESTINATION файлів для цих ключів
            new Document(
                "$lookup",
                new Document()
                    .append("from", "object_version")
                    .append("localField", "_id")
                    .append("foreignField", "key")
                    .append(
                        "pipeline",
                        List.of(
                            new Document(
                                "$match",
                                new Document()
                                    .append("s3BucketPurpose", "DESTINATION")
                                    .append("jobDefinitionGuid", jobDefinitionId)
                                    .append("bucket", bucket)
                                    .append("sourceEndpoint", sourceEndpoint)),
                            new Document(
                                "$sort", new Document("lastModified", 1)) // ASC - найстарші першими
                            ))
                    .append("as", "destinationFiles")),
            // 4. Розраховуємо логіку як в Java коді
            new Document(
                "$addFields",
                new Document()
                    .append("key", "$_id")
                    .append(
                        "actualTransferCount",
                        new Document("$min", List.of("$actualSourceCount", MAX_VERSIONS_TO_KEEP)))
                    .append("destinationCount", new Document("$size", "$destinationFiles"))),
            new Document(
                "$addFields",
                new Document()
                    .append(
                        "totalAfter",
                        new Document(
                            "$add", List.of("$actualTransferCount", "$destinationCount")))),
            new Document(
                "$addFields",
                new Document()
                    .append(
                        "toDelete",
                        new Document("$subtract", List.of("$totalAfter", MAX_VERSIONS_TO_KEEP)))),
            // 5. Фільтруємо тільки ключі де треба щось видалити (toDelete > 0)
            new Document("$match", new Document("toDelete", new Document("$gt", 0))),
            // 6. Обрізаємо DESTINATION файли до потрібної кількості (slice)
            new Document(
                "$addFields",
                new Document()
                    .append(
                        "filesToDelete",
                        new Document("$slice", List.of("$destinationFiles", "$toDelete")))),
            // 7. Розгортаємо файли для видалення
            new Document("$unwind", "$filesToDelete"),
            // 8. Повертаємо сам файл
            new Document("$replaceRoot", new Document("newRoot", "$filesToDelete")));
    // Виконуємо нативну агрегацію
    var mongoAggregation = nativeMongoAggregation(pipeline);
    return CloseableIteratorImpl.toCloseableIterator(
        mongoAggregation,
        doc -> catalogMongoTemplate.getConverter().read(ObjectVersionSnapshot.class, doc));
  }

  public CloseableIterator<ObjectVersionSnapshot> toDeleteFilesThatDontExistOnTheSource(
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
                .pipeline(
                    Aggregation.match(
                        Criteria.where("s3BucketPurpose")
                            .is(S3BucketPurpose.SOURCE)
                            .and("bucket")
                            .is(bucketName)
                            .and("sourceEndpoint")
                            .is(sourceEndpoint)
                            .and("jobDefinitionGuid")
                            .is(jobId)))
                .as("src"),
            Aggregation.match(Criteria.where("src").size(0)));
    return stream(agg);
  }

  private MongoCursor<Document> nativeMongoAggregation(List<Document> pipeline) {
    return catalogMongoTemplate
        .getCollection("object_version")
        .aggregate(pipeline, Document.class)
        .allowDiskUse(true)
        .batchSize(S3_BATCH_SIZE)
        .iterator();
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

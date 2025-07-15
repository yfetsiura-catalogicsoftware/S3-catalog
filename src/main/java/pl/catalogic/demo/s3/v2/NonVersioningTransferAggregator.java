package pl.catalogic.demo.s3.v2;


import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;
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

  // Налаштування версій файлів
  public static final int MAX_VERSIONS_TO_TRANSFER = 2; // Максимум версій що підуть на трансфер
  public static final int MAX_VERSIONS_TO_KEEP = 2; // Максимум версій що залишається на destination

  private final MongoTemplate catalogMongoTemplate;

  public NonVersioningTransferAggregator(MongoTemplate catalogMongoTemplate) {
    this.catalogMongoTemplate = catalogMongoTemplate;
  }

  public CloseableIterator<ObjectVersionSnapshot> toDeleteBeforeTransfer(
      UUID jobDefinitionId, Date fromDate, Date toDate, String bucket, String sourceEndpoint) {

    // Оптимізована версія для великих даних
    // 1. Спочатку знаходимо ключі SOURCE файлів через потік (не завантажуємо все в пам'ять)
    var sourceKeysAgg =
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
            Aggregation.group("key").count().as("sourceCount"));

    // Використовуємо потік для обробки великих даних
    var transferKeyCount = new java.util.HashMap<String, Integer>();
    try (var sourceStream =
        catalogMongoTemplate.aggregateStream(sourceKeysAgg, "object_version", Object.class)) {
      sourceStream.forEach(
          result -> {
            @SuppressWarnings("unchecked")
            var doc = (java.util.Map<String, Object>) result;
            var key = (String) doc.get("_id");
            var count = ((Number) doc.get("sourceCount")).intValue();
            transferKeyCount.put(key, count);
          });
    }

    if (transferKeyCount.isEmpty()) {
      return CloseableIteratorImpl.toCloseableIterator(Stream.empty());
    }

    // 2. Обробляємо кожен ключ окремо через Java логіку (MongoDB агрегація занадто складна)
    var filesToDelete = new ArrayList<ObjectVersionSnapshot>();

    for (var entry : transferKeyCount.entrySet()) {
      var key = entry.getKey();
      var sourceCount = entry.getValue();
      var destAgg =
          Aggregation.newAggregation(
              Aggregation.match(
                  Criteria.where("s3BucketPurpose")
                      .is(S3BucketPurpose.DESTINATION)
                      .and("jobDefinitionGuid")
                      .is(jobDefinitionId)
                      .and("bucket")
                      .is(bucket)
                      .and("sourceEndpoint")
                      .is(sourceEndpoint)
                      .and("key")
                      .is(key)),
              Aggregation.sort(Sort.by(Sort.Direction.ASC, "lastModified")));
      var destFiles =
          catalogMongoTemplate.aggregate(destAgg, "object_version", ObjectVersionSnapshot.class);
      var destList = new ArrayList<ObjectVersionSnapshot>();
      destFiles.forEach(destList::add);

      // Рахуємо скільки файлів РЕАЛЬНО піде на трансфер (max MAX_VERSIONS_TO_TRANSFER)
      var actualTransferCount = Math.min(sourceCount, MAX_VERSIONS_TO_TRANSFER);
      var totalAfterTransfer = actualTransferCount + destList.size();
      var toDeleteCount = totalAfterTransfer - MAX_VERSIONS_TO_KEEP;
      if (toDeleteCount > 0) {
        // Видаляємо найстарші DESTINATION файли
        for (int i = 0; i < Math.min(toDeleteCount, destList.size()); i++) {
          filesToDelete.add(destList.get(i));
          System.out.println(
              "  DELETE: "
                  + destList.get(i).getVersionId()
                  + " - "
                  + destList.get(i).getLastModified());
        }
      }
    }

    // 3. Повертаємо ітератор з файлами для видалення
    return CloseableIteratorImpl.toCloseableIterator(filesToDelete.stream());
  }

  //  public List<ObjectVersionSnapshot> deleteAllFromRange(UUID jobDefinitionId, String bucket) {
  //    var files = new ArrayList<ObjectVersionSnapshot>();
  //    try (var iterator = toDelete(jobDefinitionId, bucket)) {
  //      while (iterator.hasNext()) {
  //        files.add(iterator.next());
  //      }
  //    }
  //    return files;
  //  }
  //
  //  // Хелпер для тестування з часовими рамками
  //  public List<ObjectVersionSnapshot> getAllFromRange(
  //      UUID jobDefinitionId, Date fromDate, Date toDate, String bucket) {
  //    var files = new ArrayList<ObjectVersionSnapshot>();
  //    try (var iterator = toTransfer(jobDefinitionId, fromDate, toDate, bucket)) {
  //      while (iterator.hasNext()) {
  //        files.add(iterator.next());
  //      }
  //    }
  //    return files;
  //  }

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

package pl.catalogic.demo.s3.v2;


import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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

    // Простий підхід: знайти всі DESTINATION файли що треба видалити
    var filesToDelete = new ArrayList<ObjectVersionSnapshot>();
    
    // 1. Знайти ключі з SOURCE файлами в діапазоні
    var sourceKeysResult = catalogMongoTemplate.aggregate(
        Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("s3BucketPurpose").is(S3BucketPurpose.SOURCE)
                    .and("jobDefinitionGuid").is(jobDefinitionId)
                    .and("bucket").is(bucket)
                    .and("sourceEndpoint").is(sourceEndpoint)
                    .and("lastModified").gt(fromDate).lte(toDate)),
            Aggregation.group("key")),
        "object_version", Object.class);

    var sourceKeys = new ArrayList<String>();
    sourceKeysResult.forEach(result -> {
      @SuppressWarnings("unchecked")
      var doc = (java.util.Map<String, Object>) result;
      sourceKeys.add((String) doc.get("_id"));
    });

    // 2. Для кожного ключа знайти файли для видалення
    for (var key : sourceKeys) {
      // Знайти фактичну кількість SOURCE файлів для цього ключа
      var actualSourceCount = (int) catalogMongoTemplate.count(
          Query.query(
              Criteria.where("s3BucketPurpose").is(S3BucketPurpose.SOURCE)
                  .and("jobDefinitionGuid").is(jobDefinitionId)
                  .and("bucket").is(bucket)
                  .and("sourceEndpoint").is(sourceEndpoint)
                  .and("key").is(key)
                  .and("lastModified").gt(fromDate).lte(toDate)),
          ObjectVersionSnapshot.class);

      var destFiles = catalogMongoTemplate.find(
          Query.query(
              Criteria.where("s3BucketPurpose").is(S3BucketPurpose.DESTINATION)
                  .and("jobDefinitionGuid").is(jobDefinitionId)
                  .and("bucket").is(bucket)
                  .and("sourceEndpoint").is(sourceEndpoint)
                  .and("key").is(key))
              .with(Sort.by(Sort.Direction.ASC, "lastModified")),
          ObjectVersionSnapshot.class);

      // Рахувати ФАКТИЧНУ кількість що піде на трансфер
      var actualTransferCount = Math.min(actualSourceCount, MAX_VERSIONS_TO_TRANSFER);
      var totalAfter = actualTransferCount + destFiles.size();
      var toDelete = totalAfter - MAX_VERSIONS_TO_KEEP;
      
      System.out.println("Key: " + key + 
                        ", SOURCE found: " + actualSourceCount + 
                        ", will transfer: " + actualTransferCount + 
                        ", DEST: " + destFiles.size() + 
                        ", total after: " + totalAfter + 
                        ", to delete: " + toDelete);

      if (toDelete > 0) {
        var filesToDeleteForKey = destFiles.subList(0, Math.min(toDelete, destFiles.size()));
        filesToDelete.addAll(filesToDeleteForKey);
        filesToDeleteForKey.forEach(f -> 
            System.out.println("  DELETE: " + f.getVersionId() + " - " + f.getLastModified()));
      }
    }

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

package pl.catalogic.demo.s3.v2;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshot;
import pl.catalogic.demo.s3.v2.model.S3BucketPurpose;

@Service
@RequiredArgsConstructor
public class ObjectVersionService {
    
    private static final Logger log = LoggerFactory.getLogger(ObjectVersionService.class);
    private final MongoTemplate mongoTemplate;
  private static final String COLLECTION = "object_version";

  public void resetIndexes() {
    System.out.println("start " + Instant.now());
    // 1. Видалити всі індекси, крім _id_
    mongoTemplate.indexOps(COLLECTION).getIndexInfo().stream()
        .filter(index -> !"_id_".equals(index.getName()))
        .forEach(index -> mongoTemplate.indexOps(COLLECTION).dropIndex(index.getName()));
    System.out.println("done" + Instant.now());
  }

    public void updateLastModifiedByPurpose(S3BucketPurpose purpose, Instant lastModified) {
        var query = Query.query(Criteria.where("s3BucketPurpose").is(purpose));
        var update = Update.update("lastModified", lastModified);
        
        var result = mongoTemplate.updateMulti(query, update, ObjectVersionSnapshot.class);
        log.info("Updated {} documents with purpose {} to lastModified: {}", 
                result.getModifiedCount(), purpose, lastModified);
    }

    public void deleteByKeyAndPurpose(String key, S3BucketPurpose purpose) {
        var query = Query.query(
            Criteria.where("key").is(key)
                .and("s3BucketPurpose").is(purpose)
        );

        var result = mongoTemplate.remove(query, ObjectVersionSnapshot.class);
        log.info("Deleted {} documents with key={} and purpose={}", result.getDeletedCount(), key, purpose);
    }
} 
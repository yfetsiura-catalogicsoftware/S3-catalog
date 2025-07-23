package pl.catalogic.demo.s3.v2;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshot;
import pl.catalogic.demo.s3.v2.model.S3BucketPurpose;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshotRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObjectVersionService {
    
    private static final Logger log = LoggerFactory.getLogger(ObjectVersionService.class);
    private final MongoTemplate mongoTemplate;


    public void updateLastModifiedByPurpose(S3BucketPurpose purpose, Instant lastModified) {
        var query = Query.query(Criteria.where("s3BucketPurpose").is(purpose));
        var update = Update.update("lastModified", lastModified);
        
        var result = mongoTemplate.updateMulti(query, update, ObjectVersionSnapshot.class);
        log.info("Updated {} documents with purpose {} to lastModified: {}", 
                result.getModifiedCount(), purpose, lastModified);
    }
} 
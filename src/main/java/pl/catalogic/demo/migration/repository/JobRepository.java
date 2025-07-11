package pl.catalogic.demo.migration.repository;

import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.catalogic.demo.migration.entity.JobEntity;

@Repository("baseJobRepository")
public interface JobRepository extends MongoRepository<JobEntity, UUID> {}

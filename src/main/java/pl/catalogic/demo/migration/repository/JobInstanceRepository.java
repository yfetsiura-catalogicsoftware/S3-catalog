package pl.catalogic.demo.migration.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.catalogic.demo.migration.entity.JobInstance;
import pl.catalogic.demo.migration.model.JobInstanceStatus;

@Repository("baseJobInstanceRepository")
public interface JobInstanceRepository extends MongoRepository<JobInstance, UUID> {
  Optional<JobInstance> findFirstByJobInstanceExecutorIdIsNullAndStatusAndJobInstanceTypeNotIn(
      JobInstanceStatus status, List<String> excludedTypes);
}

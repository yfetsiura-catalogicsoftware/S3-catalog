package pl.catalogic.demo.migration;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MigrationHistoryRepository extends MongoRepository<MigrationHistory, UUID> {
  Optional<MigrationHistory> findFirstByOrderByAppliedOnDesc();
}

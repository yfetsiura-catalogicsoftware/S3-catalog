package pl.catalogic.demo.migration.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.catalogic.demo.migration.entity.LogEntity;

@Repository
public interface LogsRepository extends MongoRepository<LogEntity, UUID> {

}

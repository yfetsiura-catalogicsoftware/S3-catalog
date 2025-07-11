package pl.catalogic.demo.migration.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.catalogic.demo.migration.entity.S3RestoreDefinitionExtension;

@Repository
public interface S3RestoreDefinitionExtensionRepository
    extends MongoRepository<S3RestoreDefinitionExtension, UUID> {

}

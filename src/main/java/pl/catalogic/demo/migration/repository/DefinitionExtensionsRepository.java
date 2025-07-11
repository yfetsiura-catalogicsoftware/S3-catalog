package pl.catalogic.demo.migration.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.catalogic.demo.migration.entity.MultivmDefinitionExtension;

@Repository
public interface DefinitionExtensionsRepository
    extends MongoRepository<MultivmDefinitionExtension, UUID> {

  Optional<MultivmDefinitionExtension> findByJobId(UUID jobGuid);
}

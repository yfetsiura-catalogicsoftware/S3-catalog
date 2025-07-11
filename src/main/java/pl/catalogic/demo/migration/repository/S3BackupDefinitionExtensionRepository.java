package pl.catalogic.demo.migration.repository;

import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.catalogic.demo.migration.entity.S3BackupDefinitionExtension;

@Repository
public interface S3BackupDefinitionExtensionRepository
    extends MongoRepository<S3BackupDefinitionExtension, UUID> {}

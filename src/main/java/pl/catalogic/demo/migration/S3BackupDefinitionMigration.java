package pl.catalogic.demo.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.migration.entity.S3BackupDefinitionExtension;
import pl.catalogic.demo.migration.entity.S3BackupDefinitionExtension.S3BackupSource;
import pl.catalogic.demo.migration.model.S3BackupJob.Destination;
import pl.catalogic.demo.migration.repository.S3BackupDefinitionExtensionRepository;

@Component
public class S3BackupDefinitionMigration implements CollectionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3BackupDefinitionMigration.class);
  private final ObjectMapper objectMapper;
  private final S3BackupDefinitionExtensionRepository backupDefinitionExtensionRepository;

  public S3BackupDefinitionMigration(
      ObjectMapper objectMapper,
      S3BackupDefinitionExtensionRepository backupDefinitionExtensionRepository) {
    this.objectMapper = objectMapper;
    this.backupDefinitionExtensionRepository = backupDefinitionExtensionRepository;
  }

  @Override
  public void migration() {
    try {
      var resource = new ClassPathResource("mocks/DefinitionExtension.json");
      if (!resource.exists()) {
        LOGGER.warn("DefinitionExtension.json not found.");
        return;
      }

      try (InputStream inputStream = resource.getInputStream()) {
        var raw =
            objectMapper.readValue(inputStream, new TypeReference<List<Map<String, Object>>>() {});
        var backupList = new ArrayList<S3BackupDefinitionExtension>();

        raw.forEach(
            entry -> {
              if (entry.containsKey("BackupSources")) {
                backupList.add(mapToBackup(entry));
              }
            });

        backupDefinitionExtensionRepository.saveAll(backupList);
      }

    } catch (Exception e) {
      LOGGER.error("Error during migration of S3BackupDefinitionExtensions", e);
      throw new RuntimeException("Migration failed: S3BackupDefinitionExtensions", e);
    }
  }

  private S3BackupDefinitionExtension mapToBackup(Map<String, Object> raw) {
    var jobGuid = UUID.fromString((String) raw.get("_id"));
    var sourcesRaw =
        Optional.ofNullable((List<Map<String, Object>>) raw.get("BackupSources"))
            .orElse(Collections.emptyList());
    var sources = sourcesRaw.stream().map(this::toS3BackupSource).toList();
    var destination = toBackupDestination((Map<String, Object>) raw.get("BackupDestination"));

    return new S3BackupDefinitionExtension(UUID.randomUUID(), jobGuid, sources, destination);
  }

  private Destination toBackupDestination(Map<String, Object> raw) {
    var nodeId = (String) raw.get("NodeId");
    var nodeName = (String) raw.get("NodeName");
    var poolName = (String) raw.get("PoolName");

    return new Destination(nodeId, nodeName, poolName);
  }

  private S3BackupSource toS3BackupSource(Map<String, Object> raw) {
    var nodeId = (String) raw.get("NodeId");
    var nodeName = (String) raw.get("NodeName");
    @SuppressWarnings("unchecked")
    var buckets = (List<String>) raw.getOrDefault("Buckets", Collections.emptyList());
    var allBucketsAreBackupTarget =
        raw.get("AllBucketsAreBackupTarget") != null
            && (boolean) raw.get("AllBucketsAreBackupTarget");

    return new S3BackupSource(nodeId, nodeName, buckets, allBucketsAreBackupTarget);
  }
}

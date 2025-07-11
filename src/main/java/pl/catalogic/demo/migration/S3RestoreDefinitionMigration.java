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
import pl.catalogic.demo.migration.entity.S3RestoreDefinitionExtension;
import pl.catalogic.demo.migration.entity.S3RestoreDefinitionExtension.Bucket;
import pl.catalogic.demo.migration.entity.S3RestoreDefinitionExtension.DestinationNode;
import pl.catalogic.demo.migration.entity.S3RestoreDefinitionExtension.Options;
import pl.catalogic.demo.migration.entity.S3RestoreDefinitionExtension.SourceNode;
import pl.catalogic.demo.migration.repository.S3RestoreDefinitionExtensionRepository;

@Component
public class S3RestoreDefinitionMigration implements CollectionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3RestoreDefinitionMigration.class);
  private final ObjectMapper objectMapper;
  private final S3RestoreDefinitionExtensionRepository restoreDefinitionExtensionRepository;

  public S3RestoreDefinitionMigration(
      ObjectMapper objectMapper,
      S3RestoreDefinitionExtensionRepository restoreDefinitionExtensionRepository) {
    this.objectMapper = objectMapper;
    this.restoreDefinitionExtensionRepository = restoreDefinitionExtensionRepository;
  }

  @Override
  public void migration() {
    try {
      var resource = new ClassPathResource("mocks/DefinitionExtension.json");

      if (!resource.exists()) {
        LOGGER.debug("DefinitionExtension.json not found.");
        return;
      }

      try (InputStream inputStream = resource.getInputStream()) {
        var raw = objectMapper.readValue(inputStream, new TypeReference<List<Map<String, Object>>>() {});
        var restoreList = new ArrayList<S3RestoreDefinitionExtension>();

        raw.forEach(entry -> {
          if (entry.containsKey("Source")) {
            restoreList.add(restoreDefinition(entry));
          }
        });

        restoreDefinitionExtensionRepository.saveAll(restoreList);
      }

    } catch (Exception e) {
      LOGGER.error("Error during migration of S3RestoreDefinition", e);
      throw new RuntimeException("Migration failed: S3RestoreDefinition", e);
    }
  }


  private S3RestoreDefinitionExtension restoreDefinition(Map<String, Object> raw) {
    var guid = UUID.fromString((String) raw.get("Guid"));
    var sourceRaw =
        Optional.ofNullable((List<Map<String, Object>>) raw.get("Source"))
            .orElse(Collections.emptyList());
    var sources = sourceRaw.stream().map(this::toSourceNode).toList();
    var destination = toRestoreDestination((Map<String, Object>) raw.get("Destination"));
    var options = toOptions((Map<String, Object>) raw.get("JobOptions"));

    return new S3RestoreDefinitionExtension(guid, sources, destination, options);
  }

  private Options toOptions(Map<String, Object> raw) {
    var autogenerateJobName =
        raw.get("AutogenerateJobName") != null && (boolean) raw.get("AutogenerateJobName");
    var deleteRestoreJobWhenCompleted =
        raw.get("DeleteRestoreJobWhenCompleted") != null
            && (boolean) raw.get("DeleteRestoreJobWhenCompleted");

    return new Options(autogenerateJobName, deleteRestoreJobWhenCompleted);
  }

  private DestinationNode toRestoreDestination(Map<String, Object> raw) {
    var nodeId = (String) raw.get("NodeId");
    var nodeName = (String) raw.get("NodeName");

    return new DestinationNode(nodeId, nodeName);
  }

  private SourceNode toSourceNode(Map<String, Object> raw) {
    var nodeId = (String) raw.get("NodeId");
    var nodeName = (String) raw.get("NodeName");
    var bucketsRaw =
        Optional.ofNullable((List<Map<String, Object>>) raw.get("Buckets"))
            .orElse(Collections.emptyList());
    var buckets = bucketsRaw.stream().map(this::toBucket).toList();

    return new SourceNode(nodeId, nodeName, buckets);
  }

  private Bucket toBucket(Map<String, Object> raw) {
    var name = (String) raw.get("Name");
    var useLatest = raw.get("UseLatest") != null && (boolean) raw.get("UseLatest");
    var recoveryPointId = (String) raw.get("RecoveryPointId");

    return new Bucket(name, useLatest, recoveryPointId);
  }
}

package pl.catalogic.demo.migration;

import static pl.catalogic.demo.migration.InstanceMigration.extractId;
import static pl.catalogic.demo.migration.InstanceMigration.toDate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.migration.entity.JobEntity;
import pl.catalogic.demo.migration.model.JobOption;
import pl.catalogic.demo.migration.model.JobStep;
import pl.catalogic.demo.migration.model.JobTransferPath;
import pl.catalogic.demo.migration.model.JobTransferPath.Node;
import pl.catalogic.demo.migration.model.TransferPathsExclusion;
import pl.catalogic.demo.migration.model.TransferPathsExclusion.ExclusionEntry;
import pl.catalogic.demo.migration.model.TransferPathsExclusion.ExclusionPatternEntry;
import pl.catalogic.demo.migration.repository.JobRepository;

@Component
public class JobMigration implements CollectionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(JobMigration.class);
  private final JobRepository jobRepository;
  private final ObjectMapper objectMapper;

  public JobMigration(JobRepository jobRepository, ObjectMapper objectMapper) {
    this.jobRepository = jobRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void migration() {
    try {
      var resource = new ClassPathResource("mocks/Job.json");

      if (!resource.exists()) {
        LOGGER.debug("Job.json not found.");
        return;
      }

      try (InputStream inputStream = resource.getInputStream()) {
        var raw =
            objectMapper.readValue(inputStream, new TypeReference<List<Map<String, Object>>>() {});
        var jobs = raw.stream().map(this::toJob).toList();
        jobRepository.saveAll(jobs);
      }
    } catch (Exception e) {
      LOGGER.error("Error during migration of Job", e);
      throw new RuntimeException("Migration failed: Job", e);
    }
  }

  private JobEntity toJob(Map<String, Object> raw) {
    var guid = UUID.fromString(extractId(raw, "_id", "Guid"));
    var folder = (String) raw.get("Folder");
    var name = (String) raw.get("Name");
    var type = (String) raw.get("Type");
    var comment = (String) raw.get("Comment");
    var creationTime = toDate((String) raw.get("CreationTime"));
    var modificationTime = toDate((String) raw.get("ModificationTime"));
    var protectionTime = toDate((String) raw.get("ProtectionTime"));
    var creator = (String) raw.get("creator");
    var retentionDays = ((Number) raw.get("RetentionDays")).intValue();
    var recoveryPointId = (String) raw.get("RecoveryPointId");

    var optionsRaw =
        Optional.ofNullable((List<Map<String, Object>>) raw.get("Options"))
            .orElse(Collections.emptyList());
    var stepsRaw =
        Optional.ofNullable((List<Map<String, Object>>) raw.get("Steps"))
            .orElse(Collections.emptyList());

    var options = optionsRaw.stream().map(this::toJobOption).toList();
    var steps = stepsRaw.stream().map(this::toJobStep).toList();

    return new JobEntity(
        guid,
        folder,
        name,
        type,
        comment,
        creationTime,
        modificationTime,
        protectionTime,
        creator,
        options,
        steps,
        retentionDays,
        recoveryPointId);
  }

  @SuppressWarnings("unchecked")
  public JobStep toJobStep(Map<String, Object> raw) {
    var stepNumber = ((Number) raw.get("StepNumber")).intValue();
    var name = (String) raw.get("Name");
    var type = (String) raw.get("Type");
    var retentionDays = ((Number) raw.get("RetentionDays")).intValue();

    var transferPathsRaw =
        Optional.ofNullable((List<Map<String, Object>>) raw.get("TransferPaths"))
            .orElse(Collections.emptyList());
    var transferPathsExclusionRaw = (Map<String, Object>) raw.get("TransferPathsExclusion");

    var transferPaths = transferPathsRaw.stream().map(this::toJobTransferPath).toList();
    var transferPathsExclusion =
        transferPathsExclusionRaw != null
            ? toTransferPathsExclusion(transferPathsExclusionRaw)
            : null;

    return new JobStep(
        stepNumber, name, type, retentionDays, transferPaths, transferPathsExclusion);
  }

  @SuppressWarnings("unchecked")
  private TransferPathsExclusion toTransferPathsExclusion(Map<String, Object> raw) {
    var exclusionPatternEntriesRaw =
        Optional.ofNullable((List<Map<String, Object>>) raw.get("ExclusionPatternEntries"))
            .orElse(Collections.emptyList());
    var exclusionEntriesRaw =
        Optional.ofNullable((List<Map<String, Object>>) raw.get("ExclusionEntries"))
            .orElse(Collections.emptyList());

    var exclusionPatternEntries =
        exclusionPatternEntriesRaw.stream().map(this::toExclusionPatternEntry).toList();
    var exclusionEntries = exclusionEntriesRaw.stream().map(this::toExclusionEntry).toList();

    return new TransferPathsExclusion(exclusionPatternEntries, exclusionEntries);
  }

  private ExclusionPatternEntry toExclusionPatternEntry(Map<String, Object> raw) {
    var type = (String) raw.get("Type");
    var value = (String) raw.get("Value");
    var caseSensitivity =
        raw.get("CaseSensitivity") != null && (boolean) raw.get("CaseSensitivity");
    return new ExclusionPatternEntry(type, value, caseSensitivity);
  }

  @SuppressWarnings("unchecked")
  private ExclusionEntry toExclusionEntry(Map<String, Object> raw) {
    var id = extractId(raw, "_id", "Id");
    var name = (String) raw.get("Name");
    var type = (String) raw.get("Type");

    var childrenRaw =
        (List<Map<String, Object>>) raw.getOrDefault("Children", Collections.emptyList());
    var children = childrenRaw.stream().map(this::toExclusionEntry).toList();

    var options =
        Optional.ofNullable((Map<String, Object>) raw.get("Options"))
            .orElse(Collections.emptyMap())
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, e -> e.getValue() != null ? e.getValue().toString() : ""));

    return new ExclusionEntry(id, name, type, children, options);
  }

  @SuppressWarnings("unchecked")
  private JobTransferPath toJobTransferPath(Map<String, Object> raw) {
    var sourceRaw = (Map<String, Object>) raw.get("SourceNode");
    var destRaw = (Map<String, Object>) raw.get("DestinationNode");
    var sourceNode = sourceRaw != null ? toNode(sourceRaw) : null;
    var destinationNode = destRaw != null ? toNode(destRaw) : null;

    return new JobTransferPath(sourceNode, destinationNode);
  }

  @SuppressWarnings("unchecked")
  private Node toNode(Map<String, Object> raw) {
    var id = extractId(raw, "_id", "Id");
    var name = (String) raw.get("Name");
    var type = (String) raw.get("Type");
    var childrenRaw =
        (List<Map<String, Object>>) raw.getOrDefault("Children", Collections.emptyList());
    var children = childrenRaw.stream().map(this::toNode).toList();

    return new Node(id, name, type, children);
  }

  public JobOption toJobOption(Map<String, Object> raw) {
    var name = (String) raw.get("Name");
    var type = (String) raw.get("Value");

    return new JobOption(name, type);
  }
}

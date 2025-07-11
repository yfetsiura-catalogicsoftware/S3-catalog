package pl.catalogic.demo.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.migration.entity.JobInstance;
import pl.catalogic.demo.migration.model.Job;
import pl.catalogic.demo.migration.model.JobInstanceStatus;
import pl.catalogic.demo.migration.repository.JobInstanceRepository;

@Component
public class InstanceMigration implements CollectionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMigration.class);
  private final JobInstanceRepository jobInstanceRepository;
  private final ObjectMapper objectMapper;
  private final JobMigration jobMigration;

  public InstanceMigration(
      JobInstanceRepository jobInstanceRepository,
      ObjectMapper objectMapper,
      JobMigration jobMigration) {
    this.jobInstanceRepository = jobInstanceRepository;
    this.objectMapper = objectMapper;
    this.jobMigration = jobMigration;
  }

  @Override
  public void migration() {
    try {
      var resource = new ClassPathResource("mocks/JobInstance.json");

      if (!resource.exists()) {
        LOGGER.debug("JobInstance.json not found.");
        return;
      }

      try (InputStream inputStream = resource.getInputStream()) {
        var raw =
            objectMapper.readValue(inputStream, new TypeReference<List<Map<String, Object>>>() {});
        var instances = raw.stream().map(this::toJobInstance).toList();
        jobInstanceRepository.saveAll(instances);
      }

    } catch (Exception e) {
      LOGGER.error("Error during migration of Instance", e);
      throw new RuntimeException("Migration failed: Instance", e);
    }
  }

  @SuppressWarnings("unchecked")
  private JobInstance toJobInstance(Map<String, Object> raw) {
    var guid = UUID.fromString(extractId(raw, "_id", "Guid"));
    var jobName = (String) raw.get("JobName");
    var jobDisplayName = (String) raw.get("JobDisplayName");
    var jobInstanceType = (String) raw.get("JobInstanceType");
    var jobInstanceTypeGrouping = (String) raw.get("JobInstanceTypeGrouping");
    var jobInstanceCommandName = (String) raw.get("JobInstanceCommandName");
    var jobInstanceRunType = (String) raw.get("JobInstanceRunType");
    var jobInstanceExecutorId = (String) raw.get("JobInstanceExecutorId");
    var creationTime = toDate((String) raw.get("CreationTime"));
    var startTime = toDate((String) raw.get("StartTime"));
    var endTime = toDate((String) raw.get("EndTime"));
    var retentionDays = ((Number) raw.get("RetentionDays")).intValue();
    var catalogCompleted =
        raw.get("CatalogCompleted") != null && (boolean) raw.get("CatalogCompleted");
    var duration = ((Number) raw.get("Duration")).longValue();
    var totalData = ((Number) raw.get("TotalData")).longValue();
    var completedData = ((Number) raw.get("CompletedData")).longValue();
    var throughput = ((Number) raw.get("Throughput")).longValue();
    var rc = ((Number) raw.get("Rc")).longValue();
    var status =
        Optional.ofNullable((String) raw.get("Status"))
            .map(JobInstanceStatus::valueOf)
            .orElse(null);
    var taskId = ((Number) raw.get("TaskId")).intValue();
    var estimatedCompletion = toDate((String) raw.get("EstimatedCompletion"));
    var job = toJob((Map<String, Object>) raw.get("Job"));

    var builder =
        new JobInstance.Builder()
            .guid(guid)
            .jobName(jobName)
            .jobDisplayName(jobDisplayName)
            .jobInstanceType(jobInstanceType)
            .jobInstanceTypeGrouping(jobInstanceTypeGrouping)
            .jobInstanceCommandName(jobInstanceCommandName)
            .jobInstanceRunType(jobInstanceRunType)
            .jobInstanceExecutorId(jobInstanceExecutorId)
            .creationTime(creationTime)
            .startTime(startTime)
            .endTime(endTime)
            .retentionDays(retentionDays)
            .catalogCompleted(catalogCompleted)
            .duration(duration)
            .totalData(totalData)
            .completedData(completedData)
            .throughput(throughput)
            .rc(rc)
            .status(status)
            .taskId(taskId)
            .estimatedCompletion(estimatedCompletion)
            .job(job);
    return builder.build();
  }

  private Job toJob(Map<String, Object> raw) {
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

    var options = optionsRaw.stream().map(jobMigration::toJobOption).toList();
    var steps = stepsRaw.stream().map(jobMigration::toJobStep).toList();

    return new Job(
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

  public static Date toDate(String raw) {
    return raw != null ? Date.from(Instant.parse(raw)) : null;
  }

  public static String extractId(Map<String, Object> raw, String... keys) {
    for (var key : keys) {
      var value = raw.get(key);
      if (value instanceof String str && !str.isBlank()) {
        return str;
      }
    }
    throw new IllegalArgumentException("No valid ID key found in: " + List.of(keys));
  }
}

package pl.catalogic.demo.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.migration.entity.TaskEntity;
import pl.catalogic.demo.migration.model.TaskStatus;
import pl.catalogic.demo.migration.repository.TaskRepository;

@Component
public class TaskMigration implements CollectionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskMigration.class);
  private final TaskRepository taskRepository;
  private final ObjectMapper objectMapper;

  public TaskMigration(TaskRepository taskRepository, ObjectMapper objectMapper) {
    this.taskRepository = taskRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void migration() {
    try {
      var resource = new ClassPathResource("mocks/JobInstanceTask.json");
      if (!resource.exists()) {
        LOGGER.warn("Migration file mocks/JobInstanceTask.json not found. Skipping Task migration.");
        return;
      }

      try (InputStream inputStream = resource.getInputStream()) {
        var raw = objectMapper.readValue(inputStream, new TypeReference<List<Map<String, Object>>>() {});
        var tasks = raw.stream().map(this::toTaskEntity).toList();
        taskRepository.saveAll(tasks);
      }

    } catch (Exception e) {
      LOGGER.error("Error during migration of Task", e);
      throw new RuntimeException("Migration failed: Task", e);
    }
  }

  private TaskEntity toTaskEntity(Map<String, Object> raw) {
    var guid = UUID.fromString((String) raw.get("_id"));
    var jobInstanceGuid = UUID.fromString((String) raw.get("JobInstanceGuid"));
    var name = (String) raw.get("Name");
    var taskId = (String) raw.get("TaskId");
    var totalBytes = ((Number) raw.get("TotalBytes")).longValue();
    var completedBytes = ((Number) raw.get("CompletedBytes")).longValue();
    var totalUnits = ((Number) raw.get("TotalUnits")).longValue();
    var completedUnits = ((Number) raw.get("CompletedUnits")).longValue();
    var unitType = (String) raw.get("UnitType");
    var status = TaskStatus.valueOf(((String) raw.get("Status")).toUpperCase());

    var startedTime =
        Optional.ofNullable((String) raw.get("StartedTime")).map(Instant::parse).orElse(null);
    var completedTime =
        Optional.ofNullable((String) raw.get("CompletedTime")).map(Instant::parse).orElse(null);
    var createdTime =
        Optional.ofNullable((String) raw.get("CreatedTime")).map(Instant::parse).orElse(null);

    @SuppressWarnings("unchecked")
    var customProperties =
        (Map<String, String>) raw.getOrDefault("CustomProperties", Collections.emptyMap());
    return new TaskEntity(
        guid,
        jobInstanceGuid,
        name,
        taskId,
        totalBytes,
        completedBytes,
        totalUnits,
        completedUnits,
        unitType,
        status,
        startedTime,
        completedTime,
        createdTime,
        customProperties);
  }
}

package pl.catalogic.demo.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.migration.entity.LogEntity;
import pl.catalogic.demo.migration.repository.LogsRepository;

@Component
public class LogsMigration implements CollectionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(LogsMigration.class);
  private final LogsRepository logsRepository;
  private final ObjectMapper objectMapper;

  public LogsMigration(LogsRepository logsRepository, ObjectMapper objectMapper) {
    this.logsRepository = logsRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void migration() {
    try {
      var mocksDir = new ClassPathResource("mocks").getFile();
      var logFiles =
          mocksDir.listFiles((dir, name) -> name.startsWith("logs") && name.endsWith(".json"));

      if (logFiles == null) {
        LOGGER.debug("Logs.jsons not found.");
        return;
      }
      for (var file : Objects.requireNonNull(logFiles)) {
        try (InputStream inputStream = new FileInputStream(file)) {
          var rawLogs =
              objectMapper.readValue(
                  inputStream, new TypeReference<List<Map<String, Object>>>() {});
          var logEntities = rawLogs.stream().map(this::convertToLogEntity).toList();
          logsRepository.saveAll(logEntities);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Error during migration of Logs", e);
      throw new RuntimeException("Migration failed: Logs", e);
    }
  }

  private LogEntity convertToLogEntity(Map<String, Object> raw) {
    var logSize = ((Number) raw.get("LogSize")).intValue();
    var message = (String) raw.get("Message");
    var messageCode = (String) raw.get("MessageCode");
    var module = (String) raw.get("Module");
    var sourceIp = (String) raw.get("SourceIP");
    var time = (String) raw.get("Time");
    var jobInstanceId = (String) raw.get("JobInstanceId");
    var taskId = (String) raw.get("TaskId");

    if (jobInstanceId == null || jobInstanceId.isBlank()) {
      return null;
    }
    return new LogEntity(
        UUID.randomUUID(),
        logSize,
        message,
        messageCode,
        module,
        sourceIp,
        time,
        UUID.fromString(jobInstanceId),
        taskId);
  }
}

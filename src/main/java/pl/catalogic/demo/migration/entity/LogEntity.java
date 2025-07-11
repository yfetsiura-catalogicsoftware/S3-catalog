package pl.catalogic.demo.migration.entity;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Log")
public record LogEntity(
    @Id UUID id,
    int logSize,
    String message,
    String messageCode,
    String module,
    String sourceIp,
    String time,
    UUID jobInstanceId,
    String taskId) {}

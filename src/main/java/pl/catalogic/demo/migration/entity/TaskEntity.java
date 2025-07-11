package pl.catalogic.demo.migration.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pl.catalogic.demo.migration.model.TaskStatus;

@Document(collection = "Task")
public class TaskEntity {

  @Id private UUID guid;
  private UUID jobInstanceGuid;
  private String name;
  private String taskId;
  private long totalBytes;
  private long completedBytes;
  private long totalUnits;
  private long completedUnits;
  private String unitType;
  private TaskStatus status;
  private Instant startedTime;
  private Instant completedTime;
  private Instant createdTime;
  private Map<String, String> customProperties;

  public TaskEntity() {}

  public TaskEntity(
      UUID guid,
      UUID jobInstanceGuid,
      String name,
      String taskId,
      long totalBytes,
      long completedBytes,
      long totalUnits,
      long completedUnits,
      String unitType,
      TaskStatus status,
      Instant startedTime,
      Instant completedTime,
      Instant createdTime,
      Map<String, String> customProperties) {
    this.guid = guid;
    this.jobInstanceGuid = jobInstanceGuid;
    this.name = name;
    this.taskId = taskId;
    this.totalBytes = totalBytes;
    this.completedBytes = completedBytes;
    this.totalUnits = totalUnits;
    this.completedUnits = completedUnits;
    this.unitType = unitType;
    this.status = status;
    this.startedTime = startedTime;
    this.completedTime = completedTime;
    this.createdTime = createdTime;
    this.customProperties = customProperties;
  }

  public TaskEntity(
      UUID jobInstanceGuid,
      String name,
      String taskId,
      long totalBytes,
      long completedBytes,
      long totalUnits,
      long completedUnits,
      String unitType,
      TaskStatus status,
      Instant startedTime,
      Instant completedTime,
      Instant createdTime,
      Map<String, String> customProperties) {
    this(
        UUID.randomUUID(),
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

  public UUID getGuid() {
    return guid;
  }

  public void setGuid(UUID guid) {
    this.guid = guid;
  }

  public UUID getJobInstanceGuid() {
    return jobInstanceGuid;
  }

  public void setJobInstanceGuid(UUID jobInstanceGuid) {
    this.jobInstanceGuid = jobInstanceGuid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public long getTotalBytes() {
    return totalBytes;
  }

  public void setTotalBytes(long totalBytes) {
    this.totalBytes = totalBytes;
  }

  public long getCompletedBytes() {
    return completedBytes;
  }

  public void setCompletedBytes(long completedBytes) {
    this.completedBytes = completedBytes;
  }

  public long getTotalUnits() {
    return totalUnits;
  }

  public void setTotalUnits(long totalUnits) {
    this.totalUnits = totalUnits;
  }

  public long getCompletedUnits() {
    return completedUnits;
  }

  public void setCompletedUnits(long completedUnits) {
    this.completedUnits = completedUnits;
  }

  public String getUnitType() {
    return unitType;
  }

  public void setUnitType(String unitType) {
    this.unitType = unitType;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public Instant getStartedTime() {
    return startedTime;
  }

  public void setStartedTime(Instant startedTime) {
    this.startedTime = startedTime;
  }

  public Instant getCompletedTime() {
    return completedTime;
  }

  public void setCompletedTime(Instant completedTime) {
    this.completedTime = completedTime;
  }

  public Instant getCreatedTime() {
    return createdTime;
  }

  public void setCreatedTime(Instant createdTime) {
    this.createdTime = createdTime;
  }

  public Map<String, String> getCustomProperties() {
    return customProperties;
  }

  public void setCustomProperties(Map<String, String> customProperties) {
    this.customProperties = customProperties;
  }
}

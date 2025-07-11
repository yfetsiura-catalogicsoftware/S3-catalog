package pl.catalogic.demo.migration.entity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import pl.catalogic.demo.migration.model.S3BackupJob.Destination;

@Document(collection = "S3BackupDefinitionExtension")
public class S3BackupDefinitionExtension {

  @Id private UUID id;

  @Field("guid")
  private UUID jobGuid;

  private List<S3BackupSource> backupSources;
  private Destination backupDestination;

  public S3BackupDefinitionExtension() {}

  public S3BackupDefinitionExtension(
      UUID jobGuid, List<S3BackupSource> backupSources, Destination backupDestination) {
    this.jobGuid = jobGuid;
    this.backupSources = backupSources;
    this.backupDestination = backupDestination;
  }

  public S3BackupDefinitionExtension(
      UUID id, UUID jobGuid, List<S3BackupSource> backupSources, Destination backupDestination) {
    this.id = id;
    this.jobGuid = jobGuid;
    this.backupSources = backupSources;
    this.backupDestination = backupDestination;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getJobGuid() {
    return jobGuid;
  }

  public void setJobGuid(UUID jobGuid) {
    this.jobGuid = jobGuid;
  }

  public List<S3BackupSource> getBackupSources() {
    return backupSources;
  }

  public void setBackupSources(List<S3BackupSource> backupSources) {
    this.backupSources = backupSources;
  }

  public Destination getBackupDestination() {
    return backupDestination;
  }

  public void setBackupDestination(Destination backupDestination) {
    this.backupDestination = backupDestination;
  }

  public record S3BackupSource(
      String nodeId, String nodeName, List<String> buckets, boolean allBucketsAreBackupTarget) {}

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (S3BackupDefinitionExtension) o;
    return Objects.equals(id, that.id)
        && Objects.equals(jobGuid, that.jobGuid)
        && Objects.equals(backupSources, that.backupSources)
        && Objects.equals(backupDestination, that.backupDestination);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, jobGuid, backupSources, backupDestination);
  }
}

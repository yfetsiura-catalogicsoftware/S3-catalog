package pl.catalogic.demo.migration.model;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import pl.catalogic.demo.migration.entity.S3BackupDefinitionExtension.S3BackupSource;

public record S3BackupJob(
    UUID guid,
    Date creationTime,
    String name,
    String folder,
    String comment,
    List<S3BackupSource> backupSources,
    Destination backupDestination,
    Retention backupRetention) {

  public S3BackupJob(
      Date creationTime,
      String name,
      String folder,
      String comment,
      List<S3BackupSource> backupSources,
      Destination backupDestination,
      Retention backupRetention) {
    this(
        null,
        creationTime,
        name,
        folder,
        comment,
        backupSources,
        backupDestination,
        backupRetention);
  }

  public record Destination(String nodeId, String nodeName, String poolName) {}

  public record Retention(int days) {}
}

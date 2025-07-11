package pl.catalogic.demo.migration.model;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public record S3RestoreJob(
    UUID guid,
    String name,
    String folder,
    String type,
    String comment,
    Date creationTime,
    List<SourceNode> source,
    DestinationNode destination,
    Options restoreOptions) {

  public record SourceNode(String nodeId, String nodeName, List<Bucket> buckets) {}

  public record Bucket(String name, boolean useLatest, String recoveryPointId) {
    private static String determineRecoveryPointId(
        boolean useLatest, String sourceRecoveryPointId) {
      var recoveryPointId = sourceRecoveryPointId;
      if (useLatest) {
        recoveryPointId = StringUtils.EMPTY;
      } else if (StringUtils.isEmpty(recoveryPointId)) {
        throw new IllegalArgumentException("RecoveryPointId is required when UseLatest is false.");
      }
      return recoveryPointId;
    }

    public Bucket(Bucket sourceBucket) {
      this(
          sourceBucket.name(),
          sourceBucket.useLatest,
          determineRecoveryPointId(sourceBucket.useLatest, sourceBucket.recoveryPointId));
    }
  }

  public record DestinationNode(String nodeId, String nodeName) {}

  public record Options(boolean autogenerateJobName, boolean deleteRestoreJobWhenCompleted) {}

  public static String generateJobName(Clock clock) {
    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    return "s3restore-" + LocalDateTime.now(clock).format(formatter);
  }
}

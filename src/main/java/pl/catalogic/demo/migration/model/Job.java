package pl.catalogic.demo.migration.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public record Job(
    UUID guid,
    String folder,
    String name,
    String type,
    String comment,
    Date creationTime,
    Date modificationTime,
    Date protectionTime,
    String creator,
    List<JobOption> options,
    List<JobStep> steps,
    int retentionDays,
    String recoveryPointId) {

  public static class Builder {

    private UUID guid;
    private String folder;
    private String name;
    private String type;
    private String comment;
    private Date creationTime;
    private Date modificationTime;
    private Date protectionTime;
    private String creator;
    private List<JobOption> options = new ArrayList<>();
    private List<JobStep> steps = new ArrayList<>();
    private int retentionDays;
    private String recoveryPointId;

    public Builder withGeneratedUuid() {
      this.guid = UUID.randomUUID();
      return this;
    }

    public Builder setFolder(String folder) {
      this.folder = folder;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    public Builder setComment(String comment) {
      this.comment = comment;
      return this;
    }

    public Builder setCreationTime(Instant creationTime) {
      this.creationTime = Date.from(creationTime);
      return this;
    }

    public Builder setModificationTime(Instant modificationTime) {
      this.modificationTime = Date.from(modificationTime);
      return this;
    }

    public Builder setProtectionTime(Instant protectionTime) {
      this.protectionTime = Date.from(protectionTime);
      return this;
    }

    public Builder setCreator(String creator) {
      this.creator = creator;
      return this;
    }

    public Builder setOptions(List<JobOption> options) {
      this.options = options;
      return this;
    }

    public Builder setSteps(List<JobStep> steps) {
      this.steps = steps;
      return this;
    }

    public Builder setRetentionDays(int retentionDays) {
      this.retentionDays = retentionDays;
      return this;
    }

    public Builder setRecoveryPointId(String recoveryPointId) {
      this.recoveryPointId = recoveryPointId;
      return this;
    }

    public Job build() {
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
  }

  public static Builder builder() {
    return new Builder();
  }
}

package pl.catalogic.demo.migration.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pl.catalogic.demo.migration.model.JobOption;
import pl.catalogic.demo.migration.model.JobStep;

@Document(collection = "Job")
public class JobEntity {

  @Id private UUID guid;
  private String folder;
  private String name;
  private String type;
  private String comment;
  private Date creationTime;
  private Date modificationTime;
  private Date protectionTime;
  private String creator;
  private List<JobOption> options;
  private List<JobStep> steps;
  private int retentionDays;
  private String recoveryPointId;

  public JobEntity() {}

  public JobEntity(
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
    this.guid = guid;
    this.folder = folder;
    this.name = name;
    this.type = type;
    this.comment = comment;
    this.creationTime = creationTime;
    this.modificationTime = modificationTime;
    this.protectionTime = protectionTime;
    this.creator = creator;
    this.options = options;
    this.steps = steps;
    this.retentionDays = retentionDays;
    this.recoveryPointId = recoveryPointId;
  }

  public UUID getGuid() {
    return guid;
  }

  public void setGuid(UUID guid) {
    this.guid = guid;
  }

  public String getFolder() {
    return folder;
  }

  public void setFolder(String folder) {
    this.folder = folder;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Date getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Date creationTime) {
    this.creationTime = creationTime;
  }

  public Date getModificationTime() {
    return modificationTime;
  }

  public void setModificationTime(Date modificationTime) {
    this.modificationTime = modificationTime;
  }

  public Date getProtectionTime() {
    return protectionTime;
  }

  public void setProtectionTime(Date protectionTime) {
    this.protectionTime = protectionTime;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public List<JobOption> getOptions() {
    return options;
  }

  public void setOptions(List<JobOption> options) {
    this.options = options;
  }

  public List<JobStep> getSteps() {
    return steps;
  }

  public void setSteps(List<JobStep> steps) {
    this.steps = steps;
  }

  public int getRetentionDays() {
    return retentionDays;
  }

  public void setRetentionDays(int retentionDays) {
    this.retentionDays = retentionDays;
  }

  public String getRecoveryPointId() {
    return recoveryPointId;
  }

  public void setRecoveryPointId(String recoveryPointId) {
    this.recoveryPointId = recoveryPointId;
  }

  public static class Builder {

    private UUID guid = UUID.randomUUID();
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

    public JobEntity build() {
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
  }

  public static Builder builder() {
    return new Builder();
  }
}

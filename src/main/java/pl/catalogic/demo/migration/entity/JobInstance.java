package pl.catalogic.demo.migration.entity;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pl.catalogic.demo.migration.model.Job;
import pl.catalogic.demo.migration.model.JobInstanceStatus;

@Document(collection = "JobInstance")
public class JobInstance {

  public static final long ILLEGAL_INITIAL_STATE_DURATION = 0L;
  @Id private UUID guid;
  private String jobName;
  private String jobDisplayName;
  private String jobInstanceType;
  private String jobInstanceTypeGrouping;
  private String jobInstanceCommandName;
  private String jobInstanceRunType;
  private String jobInstanceExecutorId;
  private Date creationTime;
  private Date startTime;
  private Date endTime;
  private int retentionDays;
  private boolean catalogCompleted;
  private long duration;
  private long totalData;
  private long completedData;
  private long throughput;
  private long rc;
  private JobInstanceStatus status;
  private int taskId;
  private Date estimatedCompletion;
  private Job job;

  private JobInstance(Builder builder) {
    this.guid = builder.guid != null ? builder.guid : UUID.randomUUID();
    this.jobName = builder.jobName;
    this.jobDisplayName = builder.jobDisplayName;
    this.jobInstanceType = builder.jobInstanceType;
    this.jobInstanceTypeGrouping = builder.jobInstanceTypeGrouping;
    this.jobInstanceCommandName = builder.jobInstanceCommandName;
    this.jobInstanceRunType = builder.jobInstanceRunType;
    this.jobInstanceExecutorId = builder.jobInstanceExecutorId;
    this.creationTime = builder.creationTime;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.retentionDays = builder.retentionDays;
    this.catalogCompleted = builder.catalogCompleted;
    this.duration = builder.duration;
    this.totalData = builder.totalData;
    this.completedData = builder.completedData;
    this.throughput = builder.throughput;
    this.rc = builder.rc;
    this.status = builder.status;
    this.taskId = builder.taskId;
    this.estimatedCompletion = builder.estimatedCompletion;
    this.job = builder.job;
  }

  public JobInstance() {}

  public UUID getGuid() {
    return guid;
  }

  public void setGuid(UUID guid) {
    this.guid = guid;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getJobDisplayName() {
    return jobDisplayName;
  }

  public void setJobDisplayName(String jobDisplayName) {
    this.jobDisplayName = jobDisplayName;
  }

  public String getJobInstanceType() {
    return jobInstanceType;
  }

  public void setJobInstanceType(String jobInstanceType) {
    this.jobInstanceType = jobInstanceType;
  }

  public String getJobInstanceTypeGrouping() {
    return jobInstanceTypeGrouping;
  }

  public void setJobInstanceTypeGrouping(String jobInstanceTypeGrouping) {
    this.jobInstanceTypeGrouping = jobInstanceTypeGrouping;
  }

  public String getJobInstanceCommandName() {
    return jobInstanceCommandName;
  }

  public void setJobInstanceCommandName(String jobInstanceCommandName) {
    this.jobInstanceCommandName = jobInstanceCommandName;
  }

  public String getJobInstanceRunType() {
    return jobInstanceRunType;
  }

  public void setJobInstanceRunType(String jobInstanceRunType) {
    this.jobInstanceRunType = jobInstanceRunType;
  }

  public String getJobInstanceExecutorId() {
    return jobInstanceExecutorId;
  }

  public void setJobInstanceExecutorId(String jobInstanceExecutorId) {
    this.jobInstanceExecutorId = jobInstanceExecutorId;
  }

  public Date getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Instant creationTime) {
    this.creationTime = Date.from(creationTime);
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Instant startTime) {
    this.startTime = Date.from(startTime);
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Instant endTime) {
    this.endTime = Date.from(endTime);
    if (startTime != null) {
      duration = this.endTime.toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli();
    } else {
      duration = ILLEGAL_INITIAL_STATE_DURATION;
    }
  }

  public int getRetentionDays() {
    return retentionDays;
  }

  public void setRetentionDays(int retentionDays) {
    this.retentionDays = retentionDays;
  }

  public boolean isCatalogCompleted() {
    return catalogCompleted;
  }

  public void setCatalogCompleted(boolean catalogCompleted) {
    this.catalogCompleted = catalogCompleted;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public long getTotalData() {
    return totalData;
  }

  public void setTotalData(long totalData) {
    this.totalData = totalData;
  }

  public long getCompletedData() {
    return completedData;
  }

  public void setCompletedData(long completedData) {
    this.completedData = completedData;
  }

  public long getThroughput() {
    return throughput;
  }

  public void setThroughput(long throughput) {
    this.throughput = throughput;
  }

  public long getRc() {
    return rc;
  }

  public void setRc(long rc) {
    this.rc = rc;
  }

  public JobInstanceStatus getStatus() {
    return status;
  }

  public void setStatus(JobInstanceStatus status) {
    this.status = status;
  }

  public int getTaskId() {
    return taskId;
  }

  public void setTaskId(int taskId) {
    this.taskId = taskId;
  }

  public Date getEstimatedCompletion() {
    return estimatedCompletion;
  }

  public void setEstimatedCompletion(Instant estimatedCompletion) {
    this.estimatedCompletion = Date.from(estimatedCompletion);
  }

  public Job getJob() {
    return job;
  }

  public void setJob(Job job) {
    this.job = job;
  }

  public static class Builder {
    private UUID guid;
    private String jobName;
    private String jobDisplayName;
    private String jobInstanceType;
    private String jobInstanceTypeGrouping;
    private String jobInstanceCommandName;
    private String jobInstanceRunType;
    private String jobInstanceExecutorId;
    private Date creationTime;
    private Date startTime;
    private Date endTime;
    private int retentionDays;
    private boolean catalogCompleted;
    private long duration;
    private long totalData;
    private long completedData;
    private long throughput;
    private long rc;
    private JobInstanceStatus status;
    private int taskId;
    private Date estimatedCompletion;
    private Job job;

    public Builder guid(UUID guid) {
      this.guid = guid;
      return this;
    }

    public Builder jobName(String jobName) {
      this.jobName = jobName;
      return this;
    }

    public Builder jobDisplayName(String jobDisplayName) {
      this.jobDisplayName = jobDisplayName;
      return this;
    }

    public Builder jobInstanceType(String jobInstanceType) {
      this.jobInstanceType = jobInstanceType;
      return this;
    }

    public Builder jobInstanceTypeGrouping(String jobInstanceTypeGrouping) {
      this.jobInstanceTypeGrouping = jobInstanceTypeGrouping;
      return this;
    }

    public Builder jobInstanceCommandName(String jobInstanceCommandName) {
      this.jobInstanceCommandName = jobInstanceCommandName;
      return this;
    }

    public Builder jobInstanceRunType(String jobInstanceRunType) {
      this.jobInstanceRunType = jobInstanceRunType;
      return this;
    }

    public Builder jobInstanceExecutorId(String jobInstanceExecutorId) {
      this.jobInstanceExecutorId = jobInstanceExecutorId;
      return this;
    }

    public Builder creationTime(Date creationTime) {
      this.creationTime = creationTime;
      return this;
    }

    public Builder startTime(Date startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder endTime(Date endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder retentionDays(int retentionDays) {
      this.retentionDays = retentionDays;
      return this;
    }

    public Builder catalogCompleted(boolean catalogCompleted) {
      this.catalogCompleted = catalogCompleted;
      return this;
    }

    public Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public Builder totalData(long totalData) {
      this.totalData = totalData;
      return this;
    }

    public Builder completedData(long completedData) {
      this.completedData = completedData;
      return this;
    }

    public Builder throughput(long throughput) {
      this.throughput = throughput;
      return this;
    }

    public Builder rc(long rc) {
      this.rc = rc;
      return this;
    }

    public Builder status(JobInstanceStatus status) {
      this.status = status;
      return this;
    }

    public Builder taskId(int taskId) {
      this.taskId = taskId;
      return this;
    }

    public Builder estimatedCompletion(Date estimatedCompletion) {
      this.estimatedCompletion = estimatedCompletion;
      return this;
    }

    public Builder job(Job job) {
      this.job = job;
      return this;
    }

    public JobInstance build() {
      return new JobInstance(this);
    }
  }
}

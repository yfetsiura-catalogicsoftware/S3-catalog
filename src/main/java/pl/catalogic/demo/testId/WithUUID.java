package pl.catalogic.demo.testId;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "with_uuid")
public class WithUUID {

  @Id
  @JsonProperty("_id")
  private UUID id;
  private String jobName;
  private String jobDisplayName;
  private String jobInstanceType;

  public WithUUID() {}

  public WithUUID(UUID id, String jobName, String jobDisplayName, String jobInstanceType) {
    this.id = id;
    this.jobName = jobName;
    this.jobDisplayName = jobDisplayName;
    this.jobInstanceType = jobInstanceType;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getJobName() { return jobName; }
  public void setJobName(String jobName) { this.jobName = jobName; }
  public String getJobDisplayName() { return jobDisplayName; }
  public void setJobDisplayName(String jobDisplayName) { this.jobDisplayName = jobDisplayName; }
  public String getJobInstanceType() { return jobInstanceType; }
  public void setJobInstanceType(String jobInstanceType) { this.jobInstanceType = jobInstanceType; }
}

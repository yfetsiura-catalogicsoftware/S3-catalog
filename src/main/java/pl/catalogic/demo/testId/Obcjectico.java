package pl.catalogic.demo.testId;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Obcjectico")
public class Obcjectico {

  @Id private UUID guid = UUID.randomUUID();
  private String jobName;
  private IncludedObjectico includedObjectico;

  public Obcjectico() {}

  public Obcjectico(String jobName, IncludedObjectico includedObjectico) {
    this.jobName = jobName;
    this.includedObjectico = includedObjectico;
  }

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

  public IncludedObjectico getIncludedObjectico() {
    return includedObjectico;
  }

  public void setIncludedObjectico(IncludedObjectico includedObjectico) {
    this.includedObjectico = includedObjectico;
  }

  @Override
  public String toString() {
    return "Obcjectico{" +
        "guid=" + guid +
        ", jobName='" + jobName + '\'' +
        ", includedObjectico=" + includedObjectico +
        '}';
  }
}

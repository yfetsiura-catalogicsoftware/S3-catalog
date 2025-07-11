package pl.catalogic.demo.testId;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ServiceWithUUID {

  private final InstanceWithUUIDRepo repo;

  public ServiceWithUUID(InstanceWithUUIDRepo repo) {
    this.repo = repo;
  }

  public void create() {
    try {
      new WithUUID(UUID.randomUUID(), "", "", "");

      ObjectMapper objectMapper = new ObjectMapper();
      List<WithUUID> jobs = objectMapper.readValue(json, new TypeReference<List<WithUUID>>() {});

      jobs.forEach(job -> System.out.println(job.getJobName()));

      repo.saveAll(jobs);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public List<WithUUID> getAll() {
    return repo.findAll();
  }

  private String json =
      """
[
{
  "_id": "86c702e0-f655-431a-9b80-cdce3b5dc509",
  "jobName": "multi-resto",
  "jobDisplayName": "multi-resto",
   "jobInstanceType": "MULTI_VM_RESTORE"
 },
 {
   "_id": "b89c36ad-f3b6-4bcb-b99e-53cd87002d67",
   "jobName": "s3",
   "jobDisplayName": "s3",
   "jobInstanceType": "MULTI_VM_RESTORE"
 }
]

""";
}

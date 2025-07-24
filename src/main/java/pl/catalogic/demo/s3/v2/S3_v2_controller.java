package pl.catalogic.demo.s3.v2;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.catalogic.demo.s3.v2.model.S3BucketPurpose;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class S3_v2_controller {
  private final S3_v2_service service;
  private final ObjectVersionService objectVersionService;

  @GetMapping
  public ResponseEntity<String> replication() {
    var startTime = Instant.now();
    System.out.println("start aggregation " + Instant.now());
    service.getAllFroms();
    var duration = Duration.between(startTime, Instant.now());
    System.out.println("Completed toDelete aggregation in " + duration.toSeconds() + " s");
    return ResponseEntity.ok("console");
  }

  @GetMapping("/set")
  public ResponseEntity<String> set() {
    objectVersionService.updateLastModifiedByPurpose(S3BucketPurpose.SOURCE, Instant.from(Instant.parse("2023-01-01T00:00:00.00Z")));
    objectVersionService.updateLastModifiedByPurpose(S3BucketPurpose.DESTINATION, Instant.from(Instant.parse("2023-01-01T23:00:00.00Z")));
    return ResponseEntity.ok("console");
  }

  @GetMapping("/generate")
  public ResponseEntity<String> generate(@RequestParam int quantity) {
    service.generate(quantity);
    return ResponseEntity.ok("generate");
  }
  @GetMapping("/index")
  public ResponseEntity<String> index() {
    objectVersionService.resetIndexes();
    return ResponseEntity.ok("generate");
  }

  @DeleteMapping("/delete")
  public ResponseEntity<String> delete(
      @RequestParam String key,
      @RequestParam S3BucketPurpose purpose
  ) {
    objectVersionService.deleteByKeyAndPurpose(key, purpose);
    return ResponseEntity.ok("deleted");
  }


}

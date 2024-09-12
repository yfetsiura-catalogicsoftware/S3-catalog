package pl.catalogic.demo.s3.async;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replication")
@RequiredArgsConstructor
public class ReplicationController {
  private final S3ReplicationService s3ReplicationService;

  @GetMapping
  public ResponseEntity<String> replication() {
//    s3ReplicationService.replicateBucketContents("mybucket");
    return ResponseEntity.ok("Copied");
  }

//  @GetMapping("/b/{sourceBucket}/{destinationBucket}")
//  public ResponseEntity<String> replication2(@PathVariable String sourceBucket, @PathVariable String destinationBucket) {
//    s3ReplicationService.replicateObjectsMinio(sourceBucket, destinationBucket);
//    return ResponseEntity.ok("Copied");
//  }

  @GetMapping("/{sourceBucket}/{destinationBucket}")
  public ResponseEntity<String> replication(@PathVariable String sourceBucket, @PathVariable String destinationBucket) {
    s3ReplicationService.replicateObjects(sourceBucket, destinationBucket);
    return ResponseEntity.ok("Copied");
  }
}

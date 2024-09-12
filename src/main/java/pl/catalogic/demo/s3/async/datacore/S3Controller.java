package pl.catalogic.demo.s3.async.datacore;


import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
class S3Controller {

  private final S3Service s3Service;

  @GetMapping("/{sourceBucket}/{destinationBucket}")
  public ResponseEntity<String> replication(@PathVariable String sourceBucket, @PathVariable String destinationBucket) {
    s3Service.replicateObjects(sourceBucket, destinationBucket);
    return ResponseEntity.ok("Copied");
  }

  @GetMapping("/minio/details/{sourceBucket}")
  public String getBucketData(@PathVariable String sourceBucket){
    return s3Service.getMinioBucketData(sourceBucket);
  }

}

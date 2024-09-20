package pl.catalogic.demo.s3;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.catalogic.demo.s3.model.BucketResponse;
import pl.catalogic.demo.s3.model.S3ObjectResponse;
import software.amazon.awssdk.services.s3.endpoints.internal.Value.Str;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class S3Controller {

  private final Asynchro asynchro;

  @GetMapping("/buckets/backup/{sourceBucket}/{fromTo}")
  public ResponseEntity<String> backup(@PathVariable String sourceBucket, @PathVariable String fromTo) {
    asynchro.transferBucket(sourceBucket,fromTo);
    return ResponseEntity.ok("Backup started.");
  }

  @GetMapping("/buckets/{bucketName}/{client}")
  public ResponseEntity<List<S3ObjectResponse>> listObjectsList(@PathVariable String bucketName, @PathVariable String client) {
    List<S3ObjectResponse> list = asynchro.getS3Objects(bucketName, client).stream()
        .map(
            o -> new S3ObjectResponse(o.key(), o.lastModified().toString(), o.eTag(), o.size(),
                o.storageClassAsString()))
        .toList();
    return ResponseEntity.ok(list);
  }

  @GetMapping("/buckets/{client}")
  public ResponseEntity<List<BucketResponse>> listBuckets(@PathVariable String client) {
    List<BucketResponse> bucketResponses = asynchro.getBuckets(client);
    return ResponseEntity.ok(bucketResponses);
  }

}

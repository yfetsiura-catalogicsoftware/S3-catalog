package pl.catalogic.demo.s3;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.catalogic.demo.s3.model.BucketDto;
import pl.catalogic.demo.s3.model.S3ObjectDto;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class S3Controller {

  private final S3synchro synchro;

  @GetMapping("/buckets/backup/{replicatedBucketName}")
  public ResponseEntity<String> backup(@PathVariable String replicatedBucketName) {
    synchro.transferBucketObjects(replicatedBucketName, replicatedBucketName);
    return ResponseEntity.ok("Backup started");
  }

  @GetMapping("/buckets/list/{bucketName}")
  public ResponseEntity<List<S3ObjectDto>> listObjectsList(@PathVariable String bucketName) {
    List<S3ObjectDto> list =
        synchro.getAll3SObjects(bucketName).stream()
            .map(o -> new S3ObjectDto(o.key(), o.lastModified().toString(), o.eTag(), o.size()))
            .toList();
    return ResponseEntity.ok(list);
  }

  @GetMapping("/buckets")
  public ResponseEntity<List<BucketDto>> listBuckets() {
    return ResponseEntity.ok(synchro.listBuckets());
  }

}

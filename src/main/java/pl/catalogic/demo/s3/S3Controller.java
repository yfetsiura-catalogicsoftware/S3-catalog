package pl.catalogic.demo.s3;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.catalogic.demo.s3.model.BucketResponse;
import pl.catalogic.demo.s3.model.S3BackupRequest;
import pl.catalogic.demo.s3.model.S3ObjectResponse;
import pl.catalogic.demo.s3.model.S3Credentials;
import pl.catalogic.demo.s3.model.S3ObjectResponseVersion;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class S3Controller {

  private final S3Service s3Service;

  @PostMapping("/buckets/backup")
  public ResponseEntity<String> backup(@RequestBody S3BackupRequest backupRequest) {
    s3Service.backup(backupRequest);
    return ResponseEntity.ok("Backup started.");
  }


  @PostMapping("/buckets")
  public ResponseEntity<List<BucketResponse>> listBuckets(@RequestBody S3Credentials credentials) {
    List<BucketResponse> bucketResponses = s3Service.listBuckets(credentials);
    return ResponseEntity.ok(bucketResponses);
  }

  @PostMapping("/buckets/versioning/{bucketName}")
  public ResponseEntity<String> versioning(@RequestBody S3Credentials credentials, @PathVariable String bucketName) {
    s3Service.isVersioning(credentials, bucketName);
    return ResponseEntity.ok("OK");
  }

  @PostMapping("/buckets/list/versions/{bucketName}")
  public ResponseEntity<List<S3ObjectResponseVersion>> versioningFilesList(@RequestBody S3Credentials credentials, @PathVariable String bucketName) {
    List<S3ObjectResponseVersion> list = s3Service.versioningList(credentials, bucketName).stream().map(
        o -> new S3ObjectResponseVersion(o.key(), o.eTag(), o.size(), o.versionId(), o.isLatest(),
            o.lastModified().toString())).toList();
    return ResponseEntity.ok(list);
  }

  @PostMapping("/buckets/backupAll")
  public ResponseEntity<String> backupAll(@RequestBody S3BackupRequest backupRequest) {
    s3Service.buckAll(backupRequest);
    return ResponseEntity.ok("Backup started.");
  }

}

package pl.catalogic.demo.s3;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.catalogic.demo.s3.model.BucketDto;
import pl.catalogic.demo.s3.model.ObjectInfoDto;
import pl.catalogic.demo.s3.model.S3ObjectDto;
import software.amazon.awssdk.services.s3.model.ObjectVersion;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class S3Controller {

  private final S3Service s3Service;

  @GetMapping("/buckets")
  public List<BucketDto> listBuckets() {
    return s3Service.listBucketsAsync();
  }


  @GetMapping("/buckets/{bucketName}")
  public CompletableFuture<List<S3ObjectDto>> listObjects(@PathVariable String bucketName) {
    return s3Service.getAllObjects(bucketName);
  }


  @GetMapping("/buckets/backup/{replicatedBucketName}")
  public ResponseEntity<String> backup(
      @PathVariable String replicatedBucketName) {
    s3Service.transferBucket(replicatedBucketName);
    return ResponseEntity.ok("Copied");
  }

//  @GetMapping("/buckets/versions/{bucketName}")
//  public ResponseEntity<List<ObjectInfoDto>> versions(
//      @PathVariable String bucketName) {
//    List<ObjectInfoDto> list = s3Service.listObjectVersions(bucketName).stream()
//        .map(o -> new ObjectInfoDto(o.eTag(), o.size(), o.key(), o.versionId(), o.isLatest(), o.lastModified().toString()))
//        .toList();
//    return ResponseEntity.ok(list);
//  }
}

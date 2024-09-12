package pl.catalogic.demo.s3;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

  private final S3Service s3Service;



  @GetMapping("/buckets/backup/{replicatedBucketName}")
  public ResponseEntity<String> backup(@PathVariable String replicatedBucketName) {
    s3Service.transferBucket(replicatedBucketName);
    return ResponseEntity.ok("Copied");
  }


//  @GetMapping("/buckets/list/{bucketName}")
//  public ResponseEntity<String> listObjectsList(@PathVariable String bucketName) {
//    s3Service.getAllObj(bucketName);
//    return ResponseEntity.ok("List");
//  }
//
//  @GetMapping("/buckets/list2/{bucketName}")
//  public ResponseEntity<String> listObjectsList2(@PathVariable String bucketName) {
//    s3Service.getAllObj2(bucketName);
//    return ResponseEntity.ok("List");
//  }

//    @GetMapping("/buckets/versions/{bucketName}")
//    public ResponseEntity<List<ObjectInfoDto>> versions(
//        @PathVariable String bucketName) {
//      List<ObjectInfoDto> list = s3Service.listObjectVersions(bucketName).stream()
//          .map(o -> new ObjectInfoDto(o.eTag(), o.size(), o.key(), o.versionId(), o.isLatest(),
//   o.lastModified().toString()))
//          .toList();
//      return ResponseEntity.ok(list);
//    }
}

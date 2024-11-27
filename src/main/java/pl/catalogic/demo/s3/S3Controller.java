package pl.catalogic.demo.s3;

import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.catalogic.demo.s3.model.BucketDto;
import pl.catalogic.demo.s3.model.ObjectInfoDto;
import pl.catalogic.demo.s3.model.S3ObjectDto;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class S3Controller {

  private final S3Service s3Service;

  @GetMapping("/buckets")
  public List<BucketDto> listBuckets() {
    return s3Service.listBucket();
  }

  @GetMapping("/buckets2")
  public List<BucketDto> listBuckets2() {
    return s3Service.listBucket2();
  }

  @PostMapping("/buckets/{bucketName}")
  public ResponseEntity<Void> createBucket(@PathVariable String bucketName) {
    s3Service.createBucket(bucketName);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping("/buckets/{bucketName}/{minio}")
  public List<S3ObjectDto> listObjects(@PathVariable String bucketName, @PathVariable String minio) {

    return s3Service.showBucket(bucketName, minio);
  }


  @PostMapping("/buckets/upload/{bucketName}")
  public ResponseEntity<String> uploadMultipartFile(
      @PathVariable String bucketName, @RequestParam("file") MultipartFile file) {
    try {
      File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
      file.transferTo(tempFile);

      s3Service.uploadFile(bucketName, file.getOriginalFilename(), tempFile);
      tempFile.delete();

      return ResponseEntity.ok("Файл успішно завантажено на S3!");

    } catch (IOException e) {
      return ResponseEntity.status(500).body("Помилка під час завантаження: " + e.getMessage());
    }
  }

  @GetMapping("/buckets/replication/{replicatedBucketName}")
  public ResponseEntity<String> replication(
      @PathVariable String replicatedBucketName) {
    s3Service.transferBucket(replicatedBucketName);
    return ResponseEntity.ok("Copied");
  }

  @GetMapping("/buckets/versions/{bucketName}")
  public ResponseEntity<List<ObjectInfoDto>> versions(
      @PathVariable String bucketName) {
    List<ObjectInfoDto> list = s3Service.listObjectVersions(bucketName).stream()
        .map(o -> new ObjectInfoDto(o.eTag(), o.size(), o.key(), o.versionId(), o.isLatest(),
            o.lastModified().toString()))
        .toList();
    return ResponseEntity.ok(list);
  }
}

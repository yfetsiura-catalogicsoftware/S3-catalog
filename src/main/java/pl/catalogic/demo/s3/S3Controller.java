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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.catalogic.demo.s3.model.BucketDto;
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

  @PostMapping("/buckets/{bucketName}")
  public ResponseEntity<Void> createBucket(@PathVariable String bucketName) {
    s3Service.createBucket(bucketName);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping("/buckets/{bucketName}")
  public List<S3ObjectDto> listObjects(@PathVariable String bucketName) {
    return s3Service.getAllObjects(bucketName);
  }

  @GetMapping("/buckets/download")
  public String downloadAllFiles() {
    s3Service.downloadAllBuckets();
    return "Завантаження завершено!";
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

  @GetMapping("/buckets/replication/{replicatedBucketName}/{destinationBucketName}")
  public ResponseEntity<String> replication(
      @PathVariable String replicatedBucketName, @PathVariable String destinationBucketName) {
    s3Service.enableReplication(replicatedBucketName,destinationBucketName);
    return ResponseEntity.ok("Copied");
  }
}

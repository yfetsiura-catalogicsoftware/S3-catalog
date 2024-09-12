package pl.catalogic.demo.s3.async;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/multipart")
@RequiredArgsConstructor
public class MultipartController {

  private final S3MultipartUploadService multipartUploadService;

  @GetMapping("/{sourceBucket}/{destinationBucket}/{key}")
  public ResponseEntity<String> multipartUpload(@PathVariable String sourceBucket, @PathVariable String destinationBucket, @PathVariable String key)
      throws IOException {
    multipartUploadService.multipartUploadFromS3(sourceBucket, destinationBucket, key);
    return ResponseEntity.ok("Copied");
  }
}

package pl.catalogic.demo.s3;

import java.util.List;
import java.util.UUID;
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
import software.amazon.awssdk.services.s3.endpoints.internal.Value.Str;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class S3Controller {

  private final Asynchro asynchro;

  @PostMapping("/buckets/backup")
  public ResponseEntity<String> backup(@RequestBody S3BackupRequest backupRequest) {
    asynchro.backup(backupRequest);
    return ResponseEntity.ok("Backup started.");
  }
  
}

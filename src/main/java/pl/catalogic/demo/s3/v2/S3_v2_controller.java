package pl.catalogic.demo.s3.v2;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class S3_v2_controller {
  private final S3_v2_service service;

  @GetMapping
  public ResponseEntity<String> replication() {
    service.getAllFrom();
    return ResponseEntity.ok("console");
  }
}

package pl.catalogic.demo.s3.v1.model;

import java.time.Instant;

public record S3ObjectDto(
    String key, Instant lastModified, String eTag, Long size, String StorageClass) {}

package pl.catalogic.demo.s3.model;

import software.amazon.awssdk.services.s3.model.StorageClass;

public record S3ObjectResponse(
    String key, String lastModified, String eTag, Long size, String storageClass) {}

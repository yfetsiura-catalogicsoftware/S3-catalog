package pl.catalogic.demo.s3.model;

import software.amazon.awssdk.services.s3.endpoints.internal.Value.Str;

public record S3ObjectRequest(String key, Long size) {}

package pl.catalogic.demo.s3.model;

import java.time.Instant;
import software.amazon.awssdk.services.s3.endpoints.internal.Value.Str;

public record BucketDto(String name, String creationDate) {
}

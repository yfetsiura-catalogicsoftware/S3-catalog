package pl.catalogic.demo.s3.model;

import java.time.Instant;
import software.amazon.awssdk.services.s3.endpoints.internal.Value.Str;

public record ObjectInfoDto(String eTag, Long size, String key, String versionId, Boolean isLatest,String lastModified) {}

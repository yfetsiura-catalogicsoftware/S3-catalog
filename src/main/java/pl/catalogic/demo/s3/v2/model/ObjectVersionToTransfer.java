package pl.catalogic.demo.s3.v2.model;

import software.amazon.awssdk.services.s3.model.ObjectVersion;

public record ObjectVersionToTransfer(String versionId, String key, String etag, long size) {

  public ObjectVersionToTransfer(ObjectVersion objectVersion) {
    this(
        objectVersion.versionId(), objectVersion.key(), objectVersion.eTag(), objectVersion.size());
  }

  public ObjectVersionToTransfer(String versionId, ObjectVersionAggregationResult objectVersion) {
    this(
        versionId, objectVersion.key(), objectVersion.etag(), objectVersion.size());
  }
}

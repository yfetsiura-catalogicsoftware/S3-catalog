package pl.catalogic.demo.s3.v2.model;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import software.amazon.awssdk.services.s3.model.ObjectVersion;

@Document("object_version")
@CompoundIndex(
    name = "src_lookup_eq_idx",
    def  = "{'key':1,'s3BucketPurpose':1,'bucket':1,'sourceEndpoint':1,'jobDefinitionGuid':1}",
    partialFilter = "{'s3BucketPurpose':'SOURCE'}"
)
public class ObjectVersionSnapshot {

  @Id private String id;
  private String versionId;
  private String key;
  private Instant lastModified;
  private S3BucketPurpose s3BucketPurpose;
  private long size;
  private UUID jobDefinitionGuid;
  private String sourceEndpoint;
  private String bucket;

  public ObjectVersionSnapshot() {}

  public ObjectVersionSnapshot(
      String versionId,
      String key,
      Instant lastModified,
      S3BucketPurpose s3BucketPurpose,
      long size,
      UUID jobDefinitionGuid,
      String sourceEndpoint,
      String bucket) {
    this.versionId = versionId;
    this.key = key;
    this.lastModified = lastModified;
    this.s3BucketPurpose = s3BucketPurpose;
    this.size = size;
    this.jobDefinitionGuid = jobDefinitionGuid;
    this.sourceEndpoint = sourceEndpoint;
    this.bucket = bucket;
  }

  public ObjectVersionSnapshot(
      ObjectVersion objectVersion,
      S3BucketPurpose bucketPurpose,
      UUID jobDefinitionGuid,
      String sourceEndpoint,
      String bucket) {
    this.versionId = objectVersion.versionId();
    this.key = objectVersion.key();
    this.lastModified = objectVersion.lastModified();
    this.s3BucketPurpose = bucketPurpose;
    this.size = objectVersion.size();
    this.jobDefinitionGuid = jobDefinitionGuid;
    this.sourceEndpoint = sourceEndpoint;
    this.bucket = bucket;
  }

  public String getVersionId() {
    return versionId;
  }

  public void setVersionId(String versionId) {
    this.versionId = versionId;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Instant getLastModified() {
    return lastModified;
  }

  public void setLastModified(Instant lastModified) {
    this.lastModified = lastModified;
  }

  public S3BucketPurpose getS3TransferDirection() {
    return s3BucketPurpose;
  }

  public void setS3TransferDirection(S3BucketPurpose s3BucketPurpose) {
    this.s3BucketPurpose = s3BucketPurpose;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public S3BucketPurpose getS3BucketPurpose() {
    return s3BucketPurpose;
  }

  public void setS3BucketPurpose(S3BucketPurpose s3BucketPurpose) {
    this.s3BucketPurpose = s3BucketPurpose;
  }

  public UUID getJobDefinitionGuid() {
    return jobDefinitionGuid;
  }

  public void setJobDefinitionGuid(UUID jobDefinitionGuid) {
    this.jobDefinitionGuid = jobDefinitionGuid;
  }

  public String getSourceEndpoint() {
    return sourceEndpoint;
  }

  public void setSourceEndpoint(String sourceEndpoint) {
    this.sourceEndpoint = sourceEndpoint;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  @Override
  public String toString() {
    return "ObjectVersionSnapshot{" +
        "id='" + id + '\'' +
        ", versionId='" + versionId + '\'' +
        ", key='" + key + '\'' +
        ", lastModified=" + lastModified +
        ", s3BucketPurpose=" + s3BucketPurpose +
        ", size=" + size +
        ", jobDefinitionGuid='" + jobDefinitionGuid + '\'' +
        ", endpoint='" + sourceEndpoint + '\'' +
        ", bucket='" + bucket + '\'' +
        '}';
  }
}

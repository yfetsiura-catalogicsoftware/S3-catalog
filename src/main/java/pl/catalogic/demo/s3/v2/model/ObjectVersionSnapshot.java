package pl.catalogic.demo.s3.v2.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import software.amazon.awssdk.services.s3.model.ObjectVersion;

@Document("object_version")
@CompoundIndex(def = "{'versionId': 1, 'key': 1, 'etag': 1}", unique = true)
public class ObjectVersionSnapshot {

  @Id private String id;
  private String versionId;
  private String key;
  private String etag;
  private Instant lastModified;
  private S3BucketPurpose s3BucketPurpose;
  private long size;
  private String jobDefinitionGuid;
  private String endpoint;
  private String bucket;

  public ObjectVersionSnapshot() {}

  public ObjectVersionSnapshot(
      String versionId,
      String key,
      String etag,
      Instant lastModified,
      S3BucketPurpose s3BucketPurpose,
      long size,
      String jobDefinitionGuid,
      String endpoint,
      String bucket) {
    this.versionId = versionId;
    this.key = key;
    this.etag = etag;
    this.lastModified = lastModified;
    this.s3BucketPurpose = s3BucketPurpose;
    this.size = size;
    this.jobDefinitionGuid = jobDefinitionGuid;
    this.endpoint = endpoint;
    this.bucket = bucket;
  }

  public ObjectVersionSnapshot(
      ObjectVersion objectVersion,
      S3BucketPurpose bucketPurpose,
      String jobDefinitionGuid,
      String endpoint,
      String bucket) {
    this.versionId = objectVersion.versionId();
    this.key = objectVersion.key();
    this.etag = objectVersion.eTag();
    this.lastModified = objectVersion.lastModified();
    this.s3BucketPurpose = bucketPurpose;
    this.size = objectVersion.size();
    this.jobDefinitionGuid = jobDefinitionGuid;
    this.endpoint = endpoint;
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

  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
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

  public String getJobDefinitionGuid() {
    return jobDefinitionGuid;
  }

  public void setJobDefinitionGuid(String jobDefinitionGuid) {
    this.jobDefinitionGuid = jobDefinitionGuid;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
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
        ", etag='" + etag + '\'' +
        ", lastModified=" + lastModified +
        ", s3BucketPurpose=" + s3BucketPurpose +
        ", size=" + size +
        ", jobDefinitionGuid='" + jobDefinitionGuid + '\'' +
        ", endpoint='" + endpoint + '\'' +
        ", bucket='" + bucket + '\'' +
        '}';
  }
}

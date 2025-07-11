package pl.catalogic.demo.migration.entity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pl.catalogic.demo.migration.model.S3RestoreJob;

@Document(collection = "S3RestoreDefinitionExtension")
public class S3RestoreDefinitionExtension {

  @Id private UUID id = UUID.randomUUID();
  private UUID guid;
  private List<SourceNode> source;
  private DestinationNode destination;
  private Options jobOptions;

  public S3RestoreDefinitionExtension() {}

  public S3RestoreDefinitionExtension(
      UUID guid, List<SourceNode> source, DestinationNode destination, Options jobOptions) {
    this.guid = guid;
    this.source = source;
    this.destination = destination;
    this.jobOptions = jobOptions;
  }

  public S3RestoreDefinitionExtension(
      UUID guid,
      List<S3RestoreJob.SourceNode> sourceNodes,
      S3RestoreJob.DestinationNode destinationNode,
      S3RestoreJob.Options options) {
    this.guid = guid;
    this.source = sourceNodes.stream().map(SourceNode::new).toList();
    this.destination = new DestinationNode(destinationNode);
    this.jobOptions = new Options(options);
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public UUID getGuid() {
    return guid;
  }

  public void setGuid(UUID guid) {
    this.guid = guid;
  }

  public List<SourceNode> getSource() {
    return source;
  }

  public void setSource(List<SourceNode> source) {
    this.source = source;
  }

  public DestinationNode getDestination() {
    return destination;
  }

  public void setDestination(DestinationNode destination) {
    this.destination = destination;
  }

  public Options getJobOptions() {
    return jobOptions;
  }

  public void setJobOptions(Options jobOptions) {
    this.jobOptions = jobOptions;
  }

  public record SourceNode(String nodeId, String nodeName, List<Bucket> buckets) {
    public SourceNode(S3RestoreJob.SourceNode sourceNode) {
      this(
          sourceNode.nodeId(),
          sourceNode.nodeName(),
          sourceNode.buckets().stream().map(Bucket::new).toList());
    }
  }

  public record Bucket(String name, boolean useLatest, String recoveryPointId) {
    public Bucket(S3RestoreJob.Bucket bucket) {
      this(bucket.name(), bucket.useLatest(), bucket.recoveryPointId());
    }
  }

  public record DestinationNode(String nodeId, String nodeName) {
    public DestinationNode(S3RestoreJob.DestinationNode destinationNode) {
      this(destinationNode.nodeId(), destinationNode.nodeName());
    }
  }

  public record Options(boolean autogenerateJobName, boolean deleteRestoreJobWhenCompleted) {
    public Options(S3RestoreJob.Options options) {
      this(options.autogenerateJobName(), options.deleteRestoreJobWhenCompleted());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (S3RestoreDefinitionExtension) o;
    return Objects.equals(id, that.id)
        && Objects.equals(guid, that.guid)
        && Objects.equals(source, that.source)
        && Objects.equals(destination, that.destination)
        && Objects.equals(jobOptions, that.jobOptions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, guid, source, destination, jobOptions);
  }
}

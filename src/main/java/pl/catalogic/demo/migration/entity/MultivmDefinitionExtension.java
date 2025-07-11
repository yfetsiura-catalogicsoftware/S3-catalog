package pl.catalogic.demo.migration.entity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pl.catalogic.demo.migration.model.NetworkType;

@Document(collection = "MultivmDefinitionExtension")
public class MultivmDefinitionExtension {

  @Id private UUID id;
  private UUID jobId;
  private List<VmDetails> vmsArray;
  private List<NetworkRule> networkRules;
  private IpMapping ipMapping;

  public MultivmDefinitionExtension(
      UUID jobId, List<VmDetails> vmsArray, List<NetworkRule> networkRules, IpMapping ipMapping) {
    this.id = UUID.randomUUID();
    this.jobId = jobId;
    this.vmsArray = vmsArray;
    this.networkRules = networkRules;
    this.ipMapping = ipMapping;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getJobId() {
    return jobId;
  }

  public void setJobId(UUID jobId) {
    this.jobId = jobId;
  }

  public List<VmDetails> getVmsArray() {
    return vmsArray;
  }

  public void setVmsArray(List<VmDetails> vmsArray) {
    this.vmsArray = vmsArray;
  }

  public List<NetworkRule> getNetworkRules() {
    return networkRules;
  }

  public void setNetworkRules(List<NetworkRule> networkRules) {
    this.networkRules = networkRules;
  }

  public IpMapping getIpMapping() {
    return ipMapping;
  }

  public void setIpMapping(IpMapping ipMapping) {
    this.ipMapping = ipMapping;
  }

  public record VmDetails(
      String guid,
      int order,
      String recoveryPointId,
      boolean useLatest,
      String vcenterNodeName,
      String vmName) {}

  public record NetworkRule(String sourceName, Destination destination) {}

  public record Destination(String id, String name) {}

  public record IpMapping(String credentialGuid, List<IpMappingRule> rules) {}

  public record IpMappingRule(OriginalNetwork originalNetwork, TargetNetwork targetNetwork) {}

  public record OriginalNetwork(String ipRange) {}

  public record TargetNetwork(
      NetworkType type,
      String ipRange,
      String subnetMask,
      String gateway,
      List<String> dnsServers) {}
}

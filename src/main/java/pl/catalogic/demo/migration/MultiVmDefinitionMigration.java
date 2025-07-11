package pl.catalogic.demo.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.migration.entity.MultivmDefinitionExtension;
import pl.catalogic.demo.migration.entity.MultivmDefinitionExtension.Destination;
import pl.catalogic.demo.migration.entity.MultivmDefinitionExtension.IpMapping;
import pl.catalogic.demo.migration.entity.MultivmDefinitionExtension.IpMappingRule;
import pl.catalogic.demo.migration.entity.MultivmDefinitionExtension.NetworkRule;
import pl.catalogic.demo.migration.entity.MultivmDefinitionExtension.OriginalNetwork;
import pl.catalogic.demo.migration.entity.MultivmDefinitionExtension.TargetNetwork;
import pl.catalogic.demo.migration.entity.MultivmDefinitionExtension.VmDetails;
import pl.catalogic.demo.migration.model.NetworkType;
import pl.catalogic.demo.migration.repository.DefinitionExtensionsRepository;

@Component
public class MultiVmDefinitionMigration implements CollectionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(MultiVmDefinitionMigration.class);
  private final DefinitionExtensionsRepository multivmDefinitionExtensionsRepository;
  private final ObjectMapper objectMapper;

  public MultiVmDefinitionMigration(
      DefinitionExtensionsRepository multivmDefinitionExtensionsRepository,
      ObjectMapper objectMapper) {
    this.multivmDefinitionExtensionsRepository = multivmDefinitionExtensionsRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void migration() {
    try {
      var resource = new ClassPathResource("mocks/DefinitionExtensions.json");

      if (!resource.exists()) {
        LOGGER.debug("DefinitionExtensions.json not found.");
        return;
      }

      try (InputStream inputStream = resource.getInputStream()) {
        var raw =
            objectMapper.readValue(inputStream, new TypeReference<List<Map<String, Object>>>() {});
        var definitionExtensions = raw.stream().map(this::toMultivmDefinitionExtension).toList();
        multivmDefinitionExtensionsRepository.saveAll(definitionExtensions);
      }

    } catch (Exception e) {
      LOGGER.error("Error during migration of MultiVmDefinition", e);
      throw new RuntimeException("Migration failed: MultiVmDefinition", e);
    }
  }

  private MultivmDefinitionExtension toMultivmDefinitionExtension(Map<String, Object> raw) {
    var jobId = UUID.fromString((String) raw.get("_id"));

    var vmsArrayRaw =
        Optional.ofNullable((List<Map<String, Object>>) raw.get("VmsArray"))
            .orElse(Collections.emptyList());
    var networkRulesRaw =
        Optional.ofNullable((List<Map<String, Object>>) raw.get("NetworkRules"))
            .orElse(Collections.emptyList());
    var ipMappingRaw = (Map<String, Object>) raw.get("IpMapping");

    var vmDetails = vmsArrayRaw.stream().map(this::toVmDetails).toList();
    var networkRules = networkRulesRaw.stream().map(this::toNetworkRule).toList();
    var ipMapping = ipMappingRaw != null ? toIpMapping(ipMappingRaw) : null;

    return new MultivmDefinitionExtension(jobId, vmDetails, networkRules, ipMapping);
  }

  private IpMapping toIpMapping(Map<String, Object> raw) {
    var credentialGuid = (String) raw.get("CredentialGuid");

    var rulesRaw =
        Optional.ofNullable((List<Map<String, Object>>) raw.get("Rules"))
            .orElse(Collections.emptyList());

    var rules = rulesRaw.stream().map(this::toIpMappingRule).toList();

    return new IpMapping(credentialGuid, rules);
  }

  private IpMappingRule toIpMappingRule(Map<String, Object> raw) {
    var originalNetworkRaw = (Map<String, Object>) raw.get("OriginalNetwork");
    var targetNetworkRaw = (Map<String, Object>) raw.get("TargetNetwork");

    var original = toOriginalNetwork(originalNetworkRaw);
    var target = toTargetNetwork(targetNetworkRaw);

    return new IpMappingRule(original, target);
  }

  private OriginalNetwork toOriginalNetwork(Map<String, Object> raw) {
    var ipRange = (String) raw.get("IpRange");
    return new OriginalNetwork(ipRange);
  }

  private TargetNetwork toTargetNetwork(Map<String, Object> raw) {
    var type = NetworkType.valueOf(((String) raw.get("Type")).toUpperCase());
    var ipRange = (String) raw.get("IpRange");
    var subnetMask = (String) raw.get("SubnetMask");
    var gateway = (String) raw.get("Gateway");
    var dnsServers =
        Optional.ofNullable((List<String>) raw.get("DnsServers")).orElse(Collections.emptyList());
    return new TargetNetwork(type, ipRange, subnetMask, gateway, dnsServers);
  }

  private NetworkRule toNetworkRule(Map<String, Object> raw) {
    var sourceName = (String) raw.get("SourceName");
    var destinationRaw = (Map<String, Object>) raw.get("Destination");
    var id =
        Optional.ofNullable((String) destinationRaw.get("_id"))
            .orElse((String) destinationRaw.get("Id"));
    var name = (String) destinationRaw.get("Name");
    return new NetworkRule(sourceName, new Destination(id, name));
  }

  private VmDetails toVmDetails(Map<String, Object> raw) {
    var guid = (String) raw.get("Guid");
    var order = ((Number) raw.get("Order")).intValue();
    var recoveryPointId = (String) raw.get("RecoveryPointId");
    var useLatest = raw.get("UseLatest") != null && (boolean) raw.get("UseLatest");
    var vcenterNodeName = (String) raw.get("VcenterNodeName");
    var vmName = (String) raw.get("VmName");
    return new VmDetails(guid, order, recoveryPointId, useLatest, vcenterNodeName, vmName);
  }
}

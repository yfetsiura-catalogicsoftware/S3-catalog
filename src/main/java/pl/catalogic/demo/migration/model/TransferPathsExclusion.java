package pl.catalogic.demo.migration.model;

import java.util.List;
import java.util.Map;

public record TransferPathsExclusion(
    List<ExclusionPatternEntry> exclusionPatternEntries, List<ExclusionEntry> exclusionEntries) {

  public record ExclusionPatternEntry(String type, String value, boolean caseSensitivity) {}

  public record ExclusionEntry(
      String id,
      String name,
      String type,
      List<ExclusionEntry> children,
      Map<String, String> options) {}
}

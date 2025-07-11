package pl.catalogic.demo.migration;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "MigrationHistory")
public record MigrationHistory(
    @Id UUID id,
    int migrationVersion,
    Instant appliedOn,
    MigrationStatus status) {

  public enum MigrationStatus {
    SUCCESS, FAILURE
  }
}

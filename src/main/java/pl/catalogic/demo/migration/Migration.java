package pl.catalogic.demo.migration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.migration.MigrationHistory.MigrationStatus;

@Component
public class Migration {

  private static final Logger LOGGER = LoggerFactory.getLogger(Migration.class);
  private final MigrationExecutor executor;
  private final MigrationHistoryRepository migrationHistoryRepository;
  private static final int CURRENT_MIGRATION_VERSION = 1;

  public Migration(
      MigrationExecutor executor, MigrationHistoryRepository migrationHistoryRepository) {
    this.executor = executor;
    this.migrationHistoryRepository = migrationHistoryRepository;
  }

  @SneakyThrows
  @EventListener(ApplicationReadyEvent.class)
  public void migration() {
    Path dir = Path.of("C:/Users/yfetsiura/Desktop/DBmocks");
    Files.createDirectories(dir); // на всяк випадок

    Path flagFile = dir.resolve("job_db_migration_completed");
    String content = Instant.now().toString();

    Files.writeString(flagFile, content);
//    var lastMigration = migrationHistoryRepository.findFirstByOrderByAppliedOnDesc();
//    if (lastMigration.isPresent() && lastMigration.get().status() == MigrationStatus.SUCCESS) {
//      LOGGER.info("Job migration {} already successfully executed.", CURRENT_MIGRATION_VERSION);
//      return;
//    }
//
//    LOGGER.info("Migration {} starting.", CURRENT_MIGRATION_VERSION);
//    try {
//      executor.executeAllWithTransaction(CURRENT_MIGRATION_VERSION);
//    } catch (RuntimeException e) {
//      migrationHistoryRepository.insert(
//          new MigrationHistory(UUID.randomUUID(), CURRENT_MIGRATION_VERSION, Instant.now(), MigrationStatus.FAILURE));
//    }
  }

}

package pl.catalogic.demo.migration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.catalogic.demo.migration.MigrationHistory.MigrationStatus;

@Component
public class MigrationExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationExecutor.class);
  private final MigrationHistoryRepository migrationHistoryRepository;
  private final List<CollectionHandler> handlers;

  public MigrationExecutor(
      List<CollectionHandler> handlers, MigrationHistoryRepository migrationHistoryRepository) {
    this.handlers = handlers;
    this.migrationHistoryRepository = migrationHistoryRepository;
  }

  @Transactional
  public void executeAllWithTransaction(int version) {
    try {
      handlers.forEach(CollectionHandler::migration);
      migrationHistoryRepository.insert(
          new MigrationHistory(UUID.randomUUID(), version, Instant.now(), MigrationStatus.SUCCESS));
      LOGGER.info("Migration {} finished successfully.", version);
    } catch (Exception e) {
      LOGGER.error("Migration {} failed. Rolling back.", version, e);
      throw new RuntimeException("Migration failed. Rolling back.", e);
    }
  }
}

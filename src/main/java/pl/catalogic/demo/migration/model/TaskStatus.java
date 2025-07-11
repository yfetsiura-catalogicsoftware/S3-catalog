package pl.catalogic.demo.migration.model;

import pl.catalogic.demo.migration.exception.TaskUpdateException;

public enum TaskStatus {
  NOT_RUN,
  INITIALIZING,
  RUNNING,
  COMPLETED,
  FAILED;

  public TaskStatus transition(TaskStatus newStatus) {
    return switch (newStatus) {
      case INITIALIZING -> {
        if (this != NOT_RUN) {
          throw new TaskUpdateException(
              "Invalid transition: INITIALIZING only allowed from NOT_RUN");
        }
        yield newStatus;
      }
      case RUNNING -> {
        if (this != INITIALIZING) {
          throw new TaskUpdateException(
              "Invalid transition: RUNNING only allowed from INITIALIZING");
        }
        yield newStatus;
      }
      case COMPLETED -> {
        if (this != RUNNING) {
          throw new TaskUpdateException("Invalid transition: COMPLETED only allowed from RUNNING");
        }
        yield newStatus;
      }
      case FAILED -> {
        if (this != RUNNING && this != INITIALIZING) {
          throw new TaskUpdateException(
              "Invalid transition: FAILED only allowed from RUNNING or INITIALIZING");
        }
        yield newStatus;
      }
      case NOT_RUN ->
          throw new TaskUpdateException("Invalid transition: NOT_RUN cannot be set explicitly");
    };
  }
}

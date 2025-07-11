package pl.catalogic.demo.migration.model;

import java.util.List;

public enum JobInstanceStatus {
  NOT_RUN,
  RUNNING,
  SUSPENDED,
  CANCELLED,
  WAITDEPEND,
  COMPLETED,
  SUSPENDING,
  RESUMING,
  CANCELLING,
  ABORTED,
  NOT_AVAILABLE,
  PASSED,
  INITIALIZING,
  FAILED,
  ENDING,
  HELD,
  UNKNOWN;

  private static final List<JobInstanceStatus> TERMINAL_STATES =
      List.of(ABORTED, CANCELLED, ENDING, FAILED, COMPLETED, NOT_RUN);

  JobInstanceStatus transitionTo(JobInstanceStatus newStatus) {
    if (TERMINAL_STATES.contains(this)) {
      throw new IllegalStateException(
          "Job instance in state %s cannot transition to %s"
              .formatted(this.name(), newStatus.name()));
    }
    return newStatus;
  }
}

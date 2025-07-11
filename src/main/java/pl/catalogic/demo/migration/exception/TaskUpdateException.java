package pl.catalogic.demo.migration.exception;

public class TaskUpdateException extends RuntimeException {

  public TaskUpdateException(String message) {
    super(message);
  }

  public TaskUpdateException(String message, Throwable cause) {
    super(message, cause);
  }
}

package pl.catalogic.demo.s3.v2.model;

public class S3StreamException extends RuntimeException {
  public S3StreamException(Throwable cause) {
    super(cause);
  }

  public S3StreamException(String message, Throwable cause) {
    super(message, cause);
  }
}

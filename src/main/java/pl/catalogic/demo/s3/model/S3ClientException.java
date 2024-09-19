package pl.catalogic.demo.s3.model;

public class S3ClientException extends RuntimeException {

  public S3ClientException(String message) {
    super(message);
  }
}

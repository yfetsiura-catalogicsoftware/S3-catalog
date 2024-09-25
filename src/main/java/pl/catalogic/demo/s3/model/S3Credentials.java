package pl.catalogic.demo.s3.model;

public record S3Credentials(String accessKey, String secretKey, String s3accessEndpoint, String region, String bucketName) {
}

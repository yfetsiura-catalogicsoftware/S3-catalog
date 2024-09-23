package pl.catalogic.demo.s3.model;

public record BucketCredentials (S3SourceCredentials sourceCredentials, S3DestinationCredentials destinationCredentials, String bucketName) {}

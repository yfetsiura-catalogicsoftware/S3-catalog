package pl.catalogic.demo.s3.model;

public record BucketCredentials (S3Credentials sourceCredentials, S3Credentials destinationCredentials) {}

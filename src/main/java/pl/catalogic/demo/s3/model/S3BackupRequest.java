package pl.catalogic.demo.s3.model;

import java.util.List;

public record S3BackupRequest(List<BucketCredentials> buckets, String jobId) {}

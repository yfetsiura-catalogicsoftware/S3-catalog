package pl.catalogic.demo.s3.model;

public record S3ObjectResponseVersion(String key, String eTag, Long size, String versionId, Boolean isLatest, String lastModified) {

}

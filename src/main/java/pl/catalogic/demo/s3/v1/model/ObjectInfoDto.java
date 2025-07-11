package pl.catalogic.demo.s3.v1.model;

public record ObjectInfoDto(String eTag, Long size, String key, String versionId, Boolean isLatest,String lastModified) {}

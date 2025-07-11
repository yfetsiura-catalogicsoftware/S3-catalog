package pl.catalogic.demo.s3.v2.model;

import java.util.List;

public record ObjectVersionAggregationResult(
    String key,
    String etag,
    int countSource,
    int countDestination,
    int difference,
    List<String> versionIdsSource,
    List<String> versionIdsDestination,
    long size) {}

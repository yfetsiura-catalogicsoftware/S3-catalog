package pl.catalogic.demo.migration.model;

import java.util.List;

public record JobStep(
    int stepNumber,
    String name,
    String type,
    int retentionDays,
    List<JobTransferPath> transferPaths,
    TransferPathsExclusion transferPathsExclusion) {}

package pl.catalogic.demo.s3.v2;

import org.bson.Document;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class MongoSnapshotPipeline {

  private MongoSnapshotPipeline() {}

  static Document matchSource(
      UUID jobDefinitionId, Date fromDate, Date toDate, String bucket, String sourceEndpoint) {
    return new Document(
        "$match",
        new Document("s3BucketPurpose", "SOURCE")
            .append("jobDefinitionGuid", jobDefinitionId)
            .append("bucket", bucket)
            .append("sourceEndpoint", sourceEndpoint)
            .append("lastModified", new Document("$gt", fromDate).append("$lte", toDate)));
  }

  static Document sortByKeyAscAndLastModifiedDesc() {
    return new Document("$sort", new Document("key", 1).append("lastModified", -1));
  }

  static Document groupByKeyAndSourceEndpoint() {
    return new Document(
        "$group",
        new Document("_id", new Document("key", "$key").append("sourceEndpoint", "$sourceEndpoint"))
            .append("documents", new Document("$push", "$$ROOT")));
  }

  static Document projectLimitedVersions(int limit) {
    return new Document(
        "$project",
        new Document("limitedDocs", new Document("$slice", List.of("$documents", limit))));
  }

  static Document unwindLimitedDocs() {
    return new Document("$unwind", "$limitedDocs");
  }

  static Document replaceRootWithLimitedDoc() {
    return new Document("$replaceRoot", new Document("newRoot", "$limitedDocs"));
  }

  static Document finalSortByKeyAndLastModifiedAsc() {
    return new Document("$sort", new Document("key", 1).append("lastModified", 1));
  }

  static Document groupByKey() {
    return new Document(
        "$group", new Document("_id", "$key").append("actualSourceCount", new Document("$sum", 1)));
  }

  static Document lookupDestination(UUID jobDefinitionId, String bucket, String sourceEndpoint) {
    return new Document(
        "$lookup",
        new Document("from", "object_version")
            .append(
                "pipeline",
                List.of(
                    new Document(
                        "$match",
                        new Document("$expr", new Document("$eq", List.of("$key", "$$parentKey")))
                            .append("s3BucketPurpose", "DESTINATION")
                            .append("jobDefinitionGuid", jobDefinitionId)
                            .append("bucket", bucket)
                            .append("sourceEndpoint", sourceEndpoint)),
                    new Document("$sort", new Document("lastModified", 1))))
            .append("let", new Document("parentKey", "$_id"))
            .append("as", "destinationFiles"));
  }

  static Document addActualTransferCount(int maxVersionsToKeep) {
    return new Document(
        "$addFields",
        new Document("key", "$_id")
            .append(
                "actualTransferCount",
                new Document("$min", List.of("$actualSourceCount", maxVersionsToKeep))));
  }

  static Document addDestinationCount() {
    return new Document(
        "$addFields", new Document("destinationCount", new Document("$size", "$destinationFiles")));
  }

  static Document addTotalAfter() {
    return new Document(
        "$addFields",
        new Document(
            "totalAfter",
            new Document("$add", List.of("$actualTransferCount", "$destinationCount"))));
  }

  static Document addToDelete(int maxVersionsToKeep) {
    return new Document(
        "$addFields",
        new Document(
            "toDelete", new Document("$subtract", List.of("$totalAfter", maxVersionsToKeep))));
  }

  static Document filterKeysToDelete() {
    return new Document("$match", new Document("toDelete", new Document("$gt", 0)));
  }

  static Document sliceFilesToDelete() {
    return new Document(
        "$addFields",
        new Document(
            "filesToDelete", new Document("$slice", List.of("$destinationFiles", "$toDelete"))));
  }

  static Document unwindFilesToDelete() {
    return new Document("$unwind", "$filesToDelete");
  }

  static Document replaceRootFilesToDelete() {
    return new Document("$replaceRoot", new Document("newRoot", "$filesToDelete"));
  }

  static Document matchDestination(UUID jobId, String bucket, String sourceEndpoint) {
    return new Document(
        "$match",
        new Document("s3BucketPurpose", "DESTINATION")
            .append("jobDefinitionGuid", jobId)
            .append("bucket", bucket)
            .append("sourceEndpoint", sourceEndpoint));
  }

  static Document lookupSource(UUID jobId, String bucket, String sourceEndpoint) {
    return new Document(
        "$lookup",
        new Document("from", "object_version")
            .append(
                "pipeline",
                List.of(
                    new Document(
                        "$match",
                        new Document("$expr", new Document("$eq", List.of("$key", "$$parentKey")))
                            .append("s3BucketPurpose", "SOURCE")
                            .append("bucket", bucket)
                            .append("sourceEndpoint", sourceEndpoint)
                            .append("jobDefinitionGuid", jobId))))
            .append("let", new Document("parentKey", "$key"))
            .append("as", "src"));
  }

  static Document matchSrcIsEmpty() {
    return new Document("$match", new Document("src", new Document("$size", 0)));
  }

}

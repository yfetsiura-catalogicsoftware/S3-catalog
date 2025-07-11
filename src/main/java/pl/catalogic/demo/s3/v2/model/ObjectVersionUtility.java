package pl.catalogic.demo.s3.v2.model;

import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ObjectVersion;

public class ObjectVersionUtility {

  private ObjectVersionUtility() {}

  public static boolean isNotMarkedAsDeleted(
      ListObjectVersionsResponse response, ObjectVersion version) {
    return !isMarkedAsDeleted(response, version);
  }

  private static boolean isMarkedAsDeleted(
      ListObjectVersionsResponse response, ObjectVersion version) {
    return response.deleteMarkers().stream()
        .anyMatch(
            marker ->
                marker.key().equals(version.key())
                    && marker.lastModified().isAfter(version.lastModified()));
  }
}

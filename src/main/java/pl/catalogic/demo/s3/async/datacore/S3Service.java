package pl.catalogic.demo.s3.async.datacore;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ResponseMetadata;

@Service
@RequiredArgsConstructor
class S3Service {

  private final S3Client s3Client;
  private final S3Client s3ClientMinio;

  public void replicateObjects(String sourceBucket, String destinationBucket) { // working for sync
    // List objects in source bucket
    ListObjectsV2Request listObjects = ListObjectsV2Request.builder()
        .bucket(sourceBucket)
        .build();

    ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
        .bucket(sourceBucket)
        .build();
    ListObjectsResponse objectsResponse = s3Client.listObjects(listObjectsRequest);
    ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjects);
    S3ResponseMetadata s3ResponseMetadata = listObjectsResponse.responseMetadata();

    for (var s3Object : listObjectsResponse.contents()) {
      String key = s3Object.key();
      System.out.println("Copying object: " + key);
      // Get object from source bucket
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(sourceBucket)
          .key(key)
          .build();
//      s3Client.getObjectAcl()
      HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
          .bucket(sourceBucket)
          .key(key)
          .build();
      HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);

      try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(destinationBucket)
            .key(key)
            .cacheControl("www1")
            .contentDisposition("www2")
            .contentLength(10L)
            .metadata(Map.of("custom1", "value1",
                "custom2", "value2"))
            .build();

        s3ClientMinio.putObject(putObjectRequest,             RequestBody.fromInputStream(inputStream, s3Object.size()));
      } catch (Exception e) {
        System.err.println("Failed to copy object: " + key + " due to: " + e.getMessage());
      }
    }
  }


  public String getMinioBucketData(String sourceBucket) {
    ListObjectsV2Request listObjects = ListObjectsV2Request.builder()
        .bucket(sourceBucket)
        .build();
    ListObjectsV2Response listObjectsResponse = s3ClientMinio.listObjectsV2(listObjects);

    HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
        .bucket(sourceBucket)
        .key("credentials.json")
        .build();
    HeadObjectResponse headObjectResponse = s3ClientMinio.headObject(headObjectRequest);
    return headObjectResponse.toString();
  }
}

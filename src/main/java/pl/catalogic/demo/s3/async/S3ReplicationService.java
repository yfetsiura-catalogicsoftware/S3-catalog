package pl.catalogic.demo.s3.async;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
@RequiredArgsConstructor
public class S3ReplicationService {

  private final S3AsyncClient s3AsyncClientMinio;
  private final S3AsyncClient s3AsyncClient;
  private final ExecutorService executorService;

  public CompletableFuture<Void> replicateObjects(String sourceBucket, String destinationBucket) {
    ListObjectsV2Request listObjects = ListObjectsV2Request.builder()
        .bucket(sourceBucket)
        .build();

    return s3AsyncClient.listObjectsV2(listObjects)
        .thenCompose(listObjectsResponse -> {
          CompletableFuture<Void> allUploads = CompletableFuture.completedFuture(null);

          for (S3Object s3Object : listObjectsResponse.contents()) {
            String key = s3Object.key();
            System.out.println("Copying object: " + key);
            String original = s3Object.eTag();

            // Get object from source bucket asynchronously and pass it as InputStream
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(sourceBucket)
                .key(key)
                .build();


            // Use AsyncResponseTransformer.toBytes() to get object data as an InputStream
            CompletableFuture<PutObjectResponse> uploadFuture = s3AsyncClient.getObject(getObjectRequest,
                    AsyncResponseTransformer.toBlockingInputStream())
                .thenCompose(inputStream -> {
                  // Upload object to destination bucket using InputStream
                  PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                      .bucket(destinationBucket)
                      .key(key)
                      .metadata(Map.of("originalEtag", original))
                      .build();

                  // Convert InputStream to AsyncRequestBody for the destination S3 client
                  return s3AsyncClientMinio.putObject(putObjectRequest,
                      AsyncRequestBody.fromInputStream(inputStream,
                          inputStream.response().contentLength(), executorService));
                })
                .exceptionally(e -> {
                  System.err.println("Failed to copy object: " + key + " due to: " + e.getMessage());
                  return null;
                });

            // Chain upload futures to handle multiple objects sequentially
            allUploads = allUploads.thenCombine(uploadFuture, (a, b) -> null);
          }

          return allUploads;
        });
  }

//  public void replicateObjectsMinio(String sourceBucket, String destinationBucket) {
//    // List objects in source bucket
//    var listObjectsResponse = s3Client.listObjectsV2(r -> r.bucket(sourceBucket));
//
//    for (S3Object s3Object : listObjectsResponse.contents()) {
//      String key = s3Object.key();
//      System.out.println("Copying object: " + key);
//
//      // Build the CopyObjectRequest for MinIO
//      CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
//          .acl(ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
//          .sourceBucket(sourceBucket)
//          .destinationBucket(destinationBucket)
//          .sourceKey(key)
//          .destinationKey(key)
//          .build();
////      CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
////          .copySource(sourceBucket + "/" + key)
////          .bucket(destinationBucket)
////          .key(key)
////          .build();
//
//      try {
//        // Perform the copy operation
//        CopyObjectResponse copyObjectResponse = s3ClientMinio.copyObject(copyObjectRequest);
//        System.out.println("Object copied successfully: " + copyObjectResponse.copyObjectResult().eTag());
//      } catch (Exception e) {
//        System.err.println("Failed to copy object: " + key + " due to: " + e.getMessage());
//      }
//    }
//  }

//  public void replicateObjectsWithCopy(String sourceBucket, String destinationBucket) {
//    // List objects in source bucket
//    ListObjectsV2Request listObjects = ListObjectsV2Request.builder()
//        .bucket(sourceBucket)
//        .build();
//
//    ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjects);
//
//    for (var s3Object : listObjectsResponse.contents()) {
//      String key = s3Object.key();
//      System.out.println("Copying object: " + key);
//
//      // Get object from source bucket
//      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//          .bucket(sourceBucket)
//          .key(key)
//          .build();
//
//      try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
//        // Create a byte array input stream from the S3 object input stream
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
//            inputStream.readAllBytes());
//
//
//
////        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder();
////        GetBucketReplicationRequest bucketReplicationRequest = GetBucketReplicationRequest.builder()
////            .bucket(sourceBucket).build();
////        s3ClientMinio.putBucketReplication()
////        s3ClientMinio.getBucketReplication()
//        // Upload object to destination bucket
//        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//            .bucket(destinationBucket)
//            .key(key)
//            .build();
//
//        s3ClientMinio.putObject(putObjectRequest,
//            RequestBody.fromInputStream(byteArrayInputStream, byteArrayInputStream.available()));
//      } catch (Exception e) {
//        System.err.println("Failed to copy object: " + key + " due to: " + e.getMessage());
//      }
//    }
//  }



//  public void replicateBucketContents(String sourceBucket) {
//    // Pobierz listę obiektów z bucketa źródłowego
//    ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
//        .bucket(sourceBucket)
//        .build();
//
//    Bucket bucket = s3ClientMinio.listBuckets().buckets().get(0);
//    System.out.println("Bucket minio: " + bucket.name());
//
//    ListObjectsV2Response listObjectsV2Response;
//
//    do {
//      listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
//
//      // Iteruj przez obiekty w buckecie źródłowym
//      for (S3Object s3Object : listObjectsV2Response.contents()) {
//        String key = s3Object.key();
//
//        // Pobierz obiekt z bucketa źródłowego
//        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//            .bucket(sourceBucket)
//            .key(key)
//            .build();
//
//        // Skopiuj obiekt do bucketa docelowego
//        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
//            .copySource(sourceBucket + "/" + key) // Ścieżka do obiektu źródłowego
//            .destinationBucket(bucket.name())
//            .destinationKey(key)
//            .build();
//
//        // Pobierz obiekt i zapisz go w buckecie docelowym
//        CopyObjectResponse response = s3ClientMinio.copyObject(copyObjectRequest);
//        if (response.sdkHttpResponse().isSuccessful()) {
//          System.out.println("Copy successful for object: " + key);
//        } else {
//          System.out.println("Copy failed for object: " + key);
//        }
//      }
//
//      // Kontynuuj, jeśli są kolejne strony obiektów
//      listObjectsV2Request = ListObjectsV2Request.builder()
//          .bucket(sourceBucket)
//          .continuationToken(listObjectsV2Response.nextContinuationToken())
//          .build();
//
//    } while (listObjectsV2Response.isTruncated()); // Sprawdź, czy są kolejne strony
//  }
}

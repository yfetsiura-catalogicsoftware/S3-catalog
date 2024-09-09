package pl.catalogic.demo.s3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.catalogic.demo.s3.model.BucketDto;
import pl.catalogic.demo.s3.model.S3ObjectDto;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.Destination;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.PutBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ReplicationConfiguration;
import software.amazon.awssdk.services.s3.model.ReplicationRule;
import software.amazon.awssdk.services.s3.model.ReplicationRuleStatus;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@Service
@RequiredArgsConstructor
public class S3Service {

  @Value("${aws.s3.path.save}")
  private String downloadDir;
  private final S3Client s3Client;

  public List<BucketDto> listBucket() {
    List<Bucket> buckets = s3Client.listBuckets().buckets();
    return buckets.stream()
        .map(bucket -> new BucketDto(bucket.name(), bucket.creationDate()))
        .toList();
  }

  public void createBucket(String bucketName) {
    List<Bucket> buckets = s3Client.listBuckets().buckets();
    boolean bucketExists = buckets.stream().anyMatch(bucket -> bucket.name().equals(bucketName));
    if (bucketExists) {
      System.out.println("Bucket with name '" + bucketName + "' already exists.");
      return;
    }

    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    System.out.println("Bucket '" + bucketName + "' created successfully.");
  }

  public List<S3ObjectDto> getAllObjects(String bucketName) {
    List<S3ObjectDto> objects = new ArrayList<>();
    try {
      List<S3Object> contents =
          s3Client
              .listObjectsV2(ListObjectsV2Request.builder().bucket(bucketName).build())
              .contents();

      objects =
          contents.stream()
              .map(
                  o ->
                      new S3ObjectDto(
                          o.key(), o.lastModified(), o.eTag(), o.size(), o.storageClassAsString()))
              .toList();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return objects;
  }

  public void downloadAllBuckets() {
    List<Bucket> buckets = s3Client.listBuckets().buckets();

    for (Bucket bucket : buckets) {
      downloadBucket(bucket.name());
    }
  }

  public void downloadBucket(String bucketName) {
    List<S3Object> objects =
        s3Client
            .listObjectsV2(ListObjectsV2Request.builder().bucket(bucketName).build())
            .contents();

    Set<String> directories = new HashSet<>();

    // Collect all directory paths
    for (S3Object object : objects) {
      String key = object.key();
      if (key.endsWith("/")) {
        // If it's a directory, add to set
        directories.add(key);
      } else {
        // Otherwise, add the directory path
        int lastSlashIndex = key.lastIndexOf('/');
        if (lastSlashIndex > 0) { // Ensure there's a '/' character and it's not at the start
          String parentDir = key.substring(0, lastSlashIndex + 1); // Include the trailing '/'
          directories.add(parentDir);
        }
      }
    }

    // Create all directories
    for (String dir : directories) {
      Path targetDir = Paths.get(downloadDir, bucketName, dir);
      try {
        Files.createDirectories(targetDir);
        System.out.println("Created directory " + targetDir);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Download all files
    for (S3Object object : objects) {
      if (!object.key().endsWith("/")) {
        downloadObject(bucketName, object.key());
      }
    }
  }

  public void downloadObject(String bucketName, String key) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(key).build();
    Path targetPath = Paths.get(downloadDir, bucketName, key);

    try {
      Files.createDirectories(targetPath.getParent());
      s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(targetPath));
      System.out.println("Downloaded " + key + " to " + targetPath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void uploadObjectDirectly(String bucketName, MultipartFile multipartFile) {
    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(multipartFile.getOriginalFilename())
        .build();
    try (InputStream inputStream = multipartFile.getInputStream()) {
      RequestBody requestBody = RequestBody.fromInputStream(inputStream, multipartFile.getSize());
      s3Client.putObject(request, requestBody);
      System.out.println("File uploaded successfully: " + multipartFile.getOriginalFilename());
    } catch (IOException e) {
      throw new RuntimeException("Failed to upload file to S3", e);
    }
  }

  public String uploadFile(String bucketName, String keyName, File file) throws IOException {
    CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
        .bucket(bucketName)
        .key(keyName)
        .build();

    CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createRequest);
    String uploadId = response.uploadId();
    System.out.println(uploadId);
    List<CompletedPart> completedParts = new ArrayList<>();
    long partSize = 5 * 1024 * 1024; // 5 MB
    byte[] buffer = new byte[(int) partSize];
    int partNumber = 1;

    try (FileInputStream fis = new FileInputStream(file)) {
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) > 0) {
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
            .bucket(bucketName)
            .key(keyName)
            .uploadId(uploadId)
            .partNumber(partNumber)
            .build();
        UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(buffer));
        completedParts.add(CompletedPart.builder()
            .partNumber(partNumber)
            .eTag(uploadPartResponse.eTag())
            .build());
        partNumber++;
      }
    } catch (IOException e) {
      abortMultipartUpload(bucketName, keyName, uploadId);
      throw e;
    }

    CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
        .bucket(bucketName)
        .key(keyName)
        .uploadId(uploadId)
        .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
        .build();

    CompleteMultipartUploadResponse completeResponse = s3Client.completeMultipartUpload(completeRequest);
    System.out.println("Multipart upload complete");
    return completeResponse.location();
  }

  private void abortMultipartUpload(String bucketName, String keyName, String uploadId) {
    AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
        .bucket(bucketName)
        .key(keyName)
        .uploadId(uploadId)
        .build();
    s3Client.abortMultipartUpload(abortRequest);
  }

  public void enableReplication(String sourceBucketName, String destinationBucketArn) {
    String roleArn = "arn:aws:iam::123456789012:role/dcadmin";
    ReplicationConfiguration replicationConfiguration = ReplicationConfiguration.builder()
        .role(roleArn)
        .rules(ReplicationRule.builder()
            .status(ReplicationRuleStatus.ENABLED)
            .priority(1)
            .destination(Destination.builder()
                .bucket(destinationBucketArn)
                .storageClass(StorageClass.STANDARD)
                .build())
            .build())
        .build();

    PutBucketReplicationRequest request = PutBucketReplicationRequest.builder()
        .bucket(sourceBucketName)
        .replicationConfiguration(replicationConfiguration)
        .build();

    s3Client.putBucketReplication(request);
  }


}

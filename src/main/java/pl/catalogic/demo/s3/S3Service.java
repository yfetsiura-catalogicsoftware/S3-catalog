package pl.catalogic.demo.s3;

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
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

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

  public void uploadObject(String bucketName, MultipartFile multipartFile) {
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
}

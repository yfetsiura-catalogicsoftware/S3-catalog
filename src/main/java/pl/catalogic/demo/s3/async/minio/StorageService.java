//package pl.catalogic.demo.s3.minio;
//
//import io.minio.BucketExistsArgs;
//import io.minio.MakeBucketArgs;
//import io.minio.MinioClient;
//import io.minio.PutObjectArgs;
//import java.io.InputStream;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class StorageService {
//
//  @Autowired
//  private MinioClient minioClient;
//
//  public
//
//  public void uploadFile(String bucketName, String objectName, InputStream inputStream, String contentType) {
//    try {
//      boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
//      if (!found) {
//        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
//      }
//      minioClient.putObject(
//          PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
//                  inputStream, inputStream.available(), -1)
//              .contentType(contentType)
//              .build());
//    } catch (Exception e) {
//      throw new RuntimeException("Error occurred: " + e.getMessage());
//    }
//  }
//}
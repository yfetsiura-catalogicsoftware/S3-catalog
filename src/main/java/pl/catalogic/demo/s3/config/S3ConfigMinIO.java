package pl.catalogic.demo.s3.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
class S3ConfigMinIO {
  @Value("${minio.access.name}")
  private String awsAccessKey;

  @Value("${minio.access.secret}")
  private String awsSecretAccessKey;

  @Value("${aws.s3.region}")
  private String region;

  @Bean
  public S3AsyncClient s3AsyncClientMinio() {
    return S3AsyncClient.builder()
        .endpointOverride(URI.create("http://172.20.5.149:9000"))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(awsAccessKey, awsSecretAccessKey)))
        .forcePathStyle(true)
        .build();
  }

  @Bean
  public S3Client s3ClientMinio() {
    return S3Client.builder()
        .endpointOverride(URI.create("http://172.20.5.149:9000"))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(awsAccessKey, awsSecretAccessKey)))
        .forcePathStyle(true)
        .build();
  }
}

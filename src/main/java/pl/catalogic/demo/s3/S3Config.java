package pl.catalogic.demo.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {
  @Value("${aws.access.key}")
  private String awsAccessKey;

  @Value("${aws.secret.access.key}")
  private String awsSecretAccessKey;

  @Value("${aws.s3.region}")
  private String region;

  @Bean(name = "s3ClientBackup")
  public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create("http://backup42:4285"))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(awsAccessKey, awsSecretAccessKey)))
        .forcePathStyle(true)
        .build();
  }

  @Bean(name = "s3ClientTester")
  public S3Client s3ClientForTester() {
    return S3Client.builder()
        .endpointOverride(URI.create("https://catalogic-demo.cloud.datacore.com/"))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    "207eda0c56a27027e328a70388c0737f",
                    "cMnGbfQ0cZrYNwZ8gZblTxc2fTbdRNGV3giz5AtA")))
        .forcePathStyle(true)
        .build();
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
}

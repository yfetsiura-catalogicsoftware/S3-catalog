package pl.catalogic.demo.s3.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

  @Bean(name = "s3ClientBackup")
  public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create("https://192.168.129.192:9000"))
        .region(Region.of("ua"))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("lTfY4rRyKg0XeJmFuNKJ", "WXn54jZPzDJb3eNbZPNXtZZ5lonUN4MJDOri790j")))
        .forcePathStyle(false)
        .build();
  }

  @Bean(name = "s3ClientTester")
  public S3Client s3ClientForTester() {
    return S3Client.builder()
        .endpointOverride(URI.create("https://catalogic-demo.cloud.datacore.com/"))
        .region(Region.of("us"))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    "ea3ddbd5e577e44ba05a3f3420045f49",
                    "dsLtJ0A4o0xEPonba6c34G2DO93q1he4LjAoaUK4")))
        .forcePathStyle(false)
        .build();
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
}

package pl.catalogic.demo.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Configuration
public class S3Config {

  @Value("${aws.access.key.first}")
  private String awsAccessKey;

  @Value("${aws.secret.access.key.first}")
  private String awsSecretAccessKey;

  @Value("${aws.access.key.second}")
  private String awsAccessKeySecond;

  @Value("${aws.secret.access.key.second}")
  private String awsSecretAccessKeySecond;

  @Value("${aws.s3.region}")
  private String region;

  @Bean(name = "MiniPartner")
  public S3AsyncClient s3Client() {
    return S3AsyncClient.builder()
        .multipartEnabled(true)
        .endpointOverride(URI.create("http://172.20.2.219:9000"))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(awsAccessKey, awsSecretAccessKey)))
        .forcePathStyle(true)
        .overrideConfiguration(clientOverrides -> clientOverrides.apiCallTimeout(Duration.ofMinutes(2))
            .apiCallAttemptTimeout(Duration.ofSeconds(30)))
        .build();
  }

  @Bean(name = "MiniO")
  public S3AsyncClient s3ClientForTester() {
    return S3AsyncClient.builder()
        .multipartEnabled(true)
        .endpointOverride(URI.create("http://172.20.2.121:9000"))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    awsAccessKeySecond,
                    awsSecretAccessKeySecond)))
        .forcePathStyle(true)
        .overrideConfiguration(clientOverrides -> clientOverrides.apiCallTimeout(Duration.ofMinutes(2))
            .apiCallAttemptTimeout(Duration.ofSeconds(30)))
        .build();
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }
}

package pl.catalogic.demo.s3.v2;

import java.net.URI;
import java.time.Duration;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.internal.DefaultStandardRetryStrategy;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Component
public class S3Client {

  public S3AsyncClient asyncClient(String access, String secret, String endpoint) {
    var strategy = getStandardRetryStrategy();
    var configuration = asyncClientConfiguration(strategy);
    return S3AsyncClient.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of("US"))
        .httpClient(
            NettyNioAsyncHttpClient.builder()
                .connectionTimeout(Duration.ofSeconds(NETTY_CONNECTION_TIMEOUT_SECONDS))
                .tcpKeepAlive(NETTY_TCP_KEEP_ALIVE)
                .readTimeout(Duration.ofMinutes(NETTY_READ_TIMEOUT_MINUTES))
                .writeTimeout(Duration.ofMinutes(NETTY_WRITE_TIMEOUT_MINUTES))
                .connectionAcquisitionTimeout(
                    Duration.ofSeconds(NETTY_CONNECTION_ACQUISITION_TIMEOUT_SECONDS))
                .build())
        .multipartEnabled(MULTIPART_ENABLED)
        .multipartConfiguration(
            builder ->
                builder
                    .minimumPartSizeInBytes(MINIMUM_PART_SIZE)
                    .apiCallBufferSizeInBytes(API_CALL_BUFFER_SIZE_IN_BYTES))
        .forcePathStyle(false)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    access, secret)))
        .overrideConfiguration(configuration)
        .build();
  }

  private ClientOverrideConfiguration asyncClientConfiguration(StandardRetryStrategy strategy) {
    return ClientOverrideConfiguration.builder()
        .retryStrategy(strategy)
        .addExecutionInterceptor(new RemoveExpectHeaderInterceptor())
        .build();
  }

  private StandardRetryStrategy getStandardRetryStrategy() {
    var baseDelay = Duration.ofMillis(S3_RETRY_BASE_DELAY_MILLIS);
    var maxBackoff = Duration.ofMinutes(S3_RETRY_MAX_BACKOFF_MINUTES);

    var jitterBackoffStrategy = BackoffStrategy.exponentialDelayHalfJitter(baseDelay, maxBackoff);
    var throttlingBackoffStrategy =
        BackoffStrategy.exponentialDelayHalfJitter(Duration.ofSeconds(2), Duration.ofSeconds(60));

    return DefaultStandardRetryStrategy.builder()
        .maxAttempts(S3_RETRY_MAX_ATTEMPTS)
        .backoffStrategy(jitterBackoffStrategy)
        .throttlingBackoffStrategy(throttlingBackoffStrategy)
        .circuitBreakerEnabled(S3_RETRY_CIRCUIT_BREAKER_ENABLED)
        .tokenBucketExceptionCost(2)
        .tokenBucketStore(TokenBucketStore.builder().tokenBucketMaxCapacity(1000).build())
        .build();
  }

  private static final long MINIMUM_PART_SIZE = 5 * 1024 * 1024;
  private static final long API_CALL_BUFFER_SIZE_IN_BYTES = 16 * 1024 * 1024;
  private static final boolean MULTIPART_ENABLED = true;
  public static final boolean NETTY_TCP_KEEP_ALIVE = true;
  public static final int NETTY_READ_TIMEOUT_MINUTES = 15;
  public static final int NETTY_WRITE_TIMEOUT_MINUTES = 15;
  public static final int NETTY_CONNECTION_ACQUISITION_TIMEOUT_SECONDS = 40;
  public static final int NETTY_CONNECTION_TIMEOUT_SECONDS = 60;
  public static final int S3_RETRY_BASE_DELAY_MILLIS = 500;
  public static final int S3_RETRY_MAX_BACKOFF_MINUTES = 30;
  public static final int S3_RETRY_MAX_ATTEMPTS = 15;
  public static final boolean S3_RETRY_CIRCUIT_BREAKER_ENABLED = false;
}

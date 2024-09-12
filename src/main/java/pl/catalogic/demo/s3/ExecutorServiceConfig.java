package pl.catalogic.demo.s3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorServiceConfig {

  @Bean
  public ExecutorService executorService() {
    return Executors.newFixedThreadPool(10); // Możesz zmienić rozmiar puli wątków
  }
}

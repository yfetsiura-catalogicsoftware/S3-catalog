package pl.catalogic.demo.s3.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;

@Component
public class RemoveExpectHeaderInterceptor implements ExecutionInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(RemoveExpectHeaderInterceptor.class);

  @Override
  public SdkHttpRequest modifyHttpRequest(
      Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {

    var originalRequest = context.httpRequest();
    var requestBuilder = originalRequest.toBuilder();
    var expectHeader = originalRequest.firstMatchingHeader("Expect");

    if (expectHeader.isPresent() && "100-continue".equalsIgnoreCase(expectHeader.get())) {
      LOGGER.debug("Removing 'Expect' header from request");
      requestBuilder.removeHeader("Expect").build();
    }

    var request = requestBuilder.build();

    LOGGER.debug(
        "[HTTP Request] Method '{}', URI '{}', headers '{}', " + "query params '{}', port '{}'",
        request.method(),
        request.getUri(),
        request.headers(),
        request.rawQueryParameters(),
        request.port());

    return request;
  }
}

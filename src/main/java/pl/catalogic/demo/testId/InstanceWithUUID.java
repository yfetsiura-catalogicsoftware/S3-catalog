package pl.catalogic.demo.testId;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import software.amazon.awssdk.services.s3.endpoints.internal.Value.Str;

public record InstanceWithUUID(@Id UUID id, String name) {}

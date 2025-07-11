package pl.catalogic.demo.testId;

import org.springframework.data.annotation.Id;

public record InstanceWithString(@Id String id, String name) {}

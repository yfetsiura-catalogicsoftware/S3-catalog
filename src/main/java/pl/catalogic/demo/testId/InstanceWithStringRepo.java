package pl.catalogic.demo.testId;

import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InstanceWithStringRepo extends MongoRepository<InstanceWithString, String> {}

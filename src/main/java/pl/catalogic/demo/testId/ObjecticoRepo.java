package pl.catalogic.demo.testId;

import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ObjecticoRepo extends MongoRepository<Obcjectico, UUID> {

  @Query(value = "{'includedObjectico.guid': ?0}", exists = true)
  boolean existsByJobGuid(UUID guid);
}

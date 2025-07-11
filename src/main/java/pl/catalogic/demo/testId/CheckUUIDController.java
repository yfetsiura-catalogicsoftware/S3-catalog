package pl.catalogic.demo.testId;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CheckUUIDController {

  private final ServiceWithString withString;
  private final ServiceWithUUID withUUID;
  private final ObjecticoRepo objecticoRepo;

  @GetMapping("/save")
  public String saveEntities() {
    withUUID.create();
    withString.create();
    return "";
  }

  @GetMapping("/getAll")
  public List<WithUUID> getAll() {
    return withUUID.getAll();
  }

  @GetMapping("/test-request")
  public boolean testRequest(@RequestParam String jobGuid) {
    //    var obcjectico = new Obcjectico("jobo", new IncludedObjectico(UUID.randomUUID(),
    // "instancio"));
    //    objecticoRepo.insert(obcjectico);
    objecticoRepo.findAll().forEach(System.out::println);

    return objecticoRepo.existsByJobGuid(UUID.fromString(jobGuid));
  }
}

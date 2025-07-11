package pl.catalogic.demo.testId;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ServiceWithString {

  private final InstanceWithStringRepo repo;

  public ServiceWithString(InstanceWithStringRepo repo) {
    this.repo = repo;
  }


  public void create(){
//    new WithUUID();
//    var one = new InstanceWithString(null, "one");
//    var two = new InstanceWithString("b89c36ad-f3b6-4bcb-b99e-53cd87002d67", "two");
//    repo.save(one);
//    repo.save(two);
  }
}

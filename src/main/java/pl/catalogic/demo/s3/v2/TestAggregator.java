package pl.catalogic.demo.s3.v2;

import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshot;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshotRepository;
import pl.catalogic.demo.s3.v2.model.S3BucketPurpose;

@Component
@AllArgsConstructor
public class TestAggregator {
  private final ObjectVersionSnapshotRepository repository;







//  void save(){
//    var objectVersionSnapshot = new ObjectVersionSnapshot(null, "2.txt",
//        "ca867f3583059958b717d40f69b0ca8d", Instant.parse("2021-01-01T00:00:00Z"),
//        S3BucketPurpose.SOURCE, 20L, "00000000-0000-0000-0000-000000000001", "end", "bucket");
//    var objectVersionSnapshot2 = new ObjectVersionSnapshot(null, "3.txt",
//        "bee48a609f8acdfc3551fdccfb367249", Instant.parse("2021-01-01T00:29:00Z"),
//        S3BucketPurpose.SOURCE, 30L, "00000000-0000-0000-0000-000000000001", "end", "bucket");
//    var objectVersionSnapshot3 = new ObjectVersionSnapshot(null, "1.txt",
//        "0459797f5f1bc30dcdd10ce26695d679-2", Instant.parse("2021-01-01T00:29:00Z"),
//        S3BucketPurpose.DESTINATION, 10L, "00000000-0000-0000-0000-000000000001", "end", "bucket");
//    var objectVersionSnapshot4 = new ObjectVersionSnapshot(null, "2.txt",
//        "b600f8db6942903df6a53fb175d93d99-6", Instant.parse("2021-01-01T00:29:00Z"),
//        S3BucketPurpose.DESTINATION, 20L, "00000000-0000-0000-0000-000000000001", "end", "bucket");
//
//    repository.save(objectVersionSnapshot);
//    repository.save(objectVersionSnapshot2);
//    repository.save(objectVersionSnapshot3);
//    repository.save(objectVersionSnapshot4);
//  }
}

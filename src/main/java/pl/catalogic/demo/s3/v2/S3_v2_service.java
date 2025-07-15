package pl.catalogic.demo.s3.v2;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshot;
import pl.catalogic.demo.s3.v2.model.ObjectVersionSnapshotRepository;
import pl.catalogic.demo.s3.v2.model.S3BucketPurpose;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

@Component
@AllArgsConstructor
public class S3_v2_service {

  private final S3Client s3Client;
  private final TestAggregator testAggregator;
  private final NonVersioningTransferAggregator nonVersioningTransferAggregator;
  private final VersioningTransferAggregator versioningTransferAggregator;
  private static final String access = "4ftj1JPFl6VNjCejmRnq";
  private static final String secret = "2fVtMZmzA5Y17L4anicLYdx5mzLxSB91qhxBi2XU";
  private static final String end = "http://172.26.0.137:9004";
  private final ObjectVersionSnapshotRepository repository;

  @SneakyThrows
  public void getAllFrom() {
    var from = Date.from(Instant.parse("2025-07-15T09:00:00.00Z"));
    var to = Date.from(Instant.parse("2025-07-15T12:30:00.00Z"));

    //    var list = versioningTransferAggregator.toDeleteBeforeTransfer(
    //        UUID.fromString("00000000-0000-0000-0000-000000000000"),from,to,
    // "bucket","sourceEnd");
    //    var list = nonVersioningTransferAggregator.toTransfer(
    //        UUID.fromString("00000000-0000-0000-0000-000000000000"), from, to, "bucket",
    // "sourceEnd");
    //    var list = nonVersioningTransferAggregator.toDelete(
    //        UUID.fromString("00000000-0000-0000-0000-000000000000"), "bucket", "sourceEnd");
//    var list =
//        versioningTransferAggregator.toTransfer(
//            UUID.fromString("00000000-0000-0000-0000-000000000000"),
//            from,
//            to,
//            "bucket",
//            "sourceEnd");
    var list =
        versioningTransferAggregator.toDeleteBeforeTransfer(
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            from,
            to,
            "bucket",
            "sourceEnd");

    System.out.println("---------files------------------");
    list.stream().forEach(System.out::println);
    System.out.println("---------------------------");
  }

  public void getAllFroms() {

    var source =
        s3Client.asyncClient(
            "NlN799TBMSVDJC60xgaD",
            "KF8MKLaZ6oxW6LE1DcjtAWXJPtN1mYp1uhjMfEJs",
            "http://172.26.0.137:9000/");

        var sourceContent =
            source
                .listObjectsV2(ListObjectsV2Request.builder().bucket("versioning").build())
                .join()
                .contents();

    source
        .listObjectVersionsPaginator(builder -> builder.bucket("versioning"))
        .subscribe(
            versions -> {
              versions
                  .versions()
                  .forEach(
                      o ->
                          repository.save(
                              new ObjectVersionSnapshot(
                                  o.versionId(),
                                  o.key(),
                                  o.lastModified(),
                                  S3BucketPurpose.SOURCE,
                                  o.size(),
                                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                                  "sourceEnd",
                                  "bucket")));
            });

    //    var desti = s3Client.asyncClient("i10hIqm3NG2jrzPsbZSL",
    //        "gPyPsj0MThjiovhDNSYMeeFactLmGbjcmJUrfQsT", "http://172.26.0.137:9002/");

    //    var desti2 = s3Client.asyncClient(access, secret, end);
    //
    //    desti2
    //        .listObjectVersionsPaginator(builder -> builder.bucket("non-versioning"))
    //        .subscribe(
    //            versions -> {
    //              versions.versions().stream().forEach(System.out::println);
    //            });
  }
}

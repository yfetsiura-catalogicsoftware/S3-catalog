package pl.catalogic.demo.s3.v2;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

@Component
@AllArgsConstructor
public class S3_v2_service {

  private final S3Client s3Client;
  private final TestAggregator testAggregator;
  private final NonVersioningTransferAggregator nonVersioningTransferAggregator;
  private static final String access = "4ftj1JPFl6VNjCejmRnq";
  private static final String secret = "2fVtMZmzA5Y17L4anicLYdx5mzLxSB91qhxBi2XU";
  private static final String end = "http://172.26.0.137:9004";

  @SneakyThrows
  public void getAllFrom(boolean test) {}

  public void getAllFrom() {

    var source =
        s3Client.asyncClient(
            "NlN799TBMSVDJC60xgaD",
            "KF8MKLaZ6oxW6LE1DcjtAWXJPtN1mYp1uhjMfEJs",
            "http://172.26.0.137:9000/");

//    var sourceContent =
//        source
//            .listObjectsV2(ListObjectsV2Request.builder().bucket("versioning").build())
//            .join()
//            .contents();

    source
        .listObjectVersionsPaginator(builder -> builder.bucket("versioning"))
        .subscribe(
            versions -> {
              versions.versions().forEach(System.out::println);
              System.out.println("--------");
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

package pl.catalogic.demo.s3;

import java.util.List;
import pl.catalogic.demo.s3.model.BucketResponse;
import pl.catalogic.demo.s3.model.S3BackupRequest;
import pl.catalogic.demo.s3.model.S3Credentials;

public interface S3ManagerService {

  List<BucketResponse> getBuckets(S3Credentials credentials);

  void transferBucket(S3BackupRequest copyRequest);

}

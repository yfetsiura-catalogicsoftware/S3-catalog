package pl.catalogic.demo.s3;

import java.io.InputStream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class S3synchro {
  private final S3Client s3Client;
  private final S3Client s3ClientTester;
  private static final long PART_SIZE = 5 * 1024 * 1024; // 5MB частини

  public S3synchro(
      @Qualifier("s3ClientBackupSyn") S3Client s3Client,
      @Qualifier("s3ClientTesterSyn") S3Client s3ClientTester) {
    this.s3Client = s3Client;
    this.s3ClientTester = s3ClientTester;
  }

  public void transferFile(String sourceBucket) {

    ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
        .bucket(sourceBucket)
        .build();

    ListObjectsV2Response listObjectsResponse;
    do {
      // Пагінація для отримання всіх об'єктів
      listObjectsResponse = s3Client.listObjectsV2(listObjectsV2Request);

      // Для кожного об'єкта виконуємо копіювання
      for (S3Object s3Object : listObjectsResponse.contents()) {
        transferObject(sourceBucket, s3Object.key(), destinationBucket);
      }

      // Продовжуємо обробку, якщо є ще об'єкти
      listObjectsV2Request = listObjectsV2Request.toBuilder()
          .continuationToken(listObjectsResponse.nextContinuationToken())
          .build();

    } while (listObjectsResponse.isTruncated());
  }

  private void transferObject(String sourceBucket, String objectKey, String destinationBucket) {
    // Створюємо запит на отримання об'єкта
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(sourceBucket)
        .key(objectKey)
        .build();

    // Отримуємо потік об'єкта з джерела
    try (InputStream inputStream = s3ClientTester.getObject(getObjectRequest)) {

      // Створюємо запит на завантаження в новий бакет
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(destinationBucket)
          .key(objectKey)
          .build();

      // Передаємо потік даних у новий бакет
      s3ClientTester.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, inputStream.available()));

    } catch (Exception e) {
      e.printStackTrace();
      // Обробка помилок
    }
  }
}

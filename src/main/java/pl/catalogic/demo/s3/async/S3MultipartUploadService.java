package pl.catalogic.demo.s3.async;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@Service
@RequiredArgsConstructor
public class S3MultipartUploadService {
  private static final long PART_SIZE = 5 * 1024 * 1024;
  private final S3AsyncClient s3AsyncClientMinio;
  private final S3AsyncClient s3AsyncClient;
  private final ExecutorService executorService;

  public void multipartUploadFromS3(String sourceBucket, String destinationBucket, String key)
      throws IOException {
    // Krok 1: Pobierz obiekt jako InputStream z S3
    CompletableFuture<ResponseInputStream<GetObjectResponse>> getObjectResponseFuture = s3AsyncClient.getObject(
        GetObjectRequest.builder()
        .bucket(sourceBucket)
        .key(key)
        .build(), AsyncResponseTransformer.toBlockingInputStream());

    InputStream inputStream = getObjectResponseFuture.join();

    // Krok 2: Inicjuj multipart upload na nowy bucket
    CompletableFuture<CreateMultipartUploadResponse> createResponseFuture = s3AsyncClientMinio.createMultipartUpload(
        CreateMultipartUploadRequest.builder()
            .bucket(destinationBucket)
            .key(key)
            .build());

    String uploadId = createResponseFuture.join().uploadId();
    System.out.println("Upload initiated with uploadId: " + uploadId);

    List<CompletedPart> completedParts = new ArrayList<>();
    byte[] buffer = new byte[(int) PART_SIZE];
    int bytesRead;
    int partNumber = 1;

    // Krok 3: Odczytaj InputStream w częściach i przesyłaj
    while ((bytesRead = inputStream.read(buffer)) > 0) {
      ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);

      AsyncRequestBody requestBody = AsyncRequestBody.fromByteBuffer(byteBuffer);
      CompletableFuture<UploadPartResponse> uploadPartResponseFuture = s3AsyncClientMinio.uploadPart(
          UploadPartRequest.builder()
              .bucket(destinationBucket)
              .key(key)
              .uploadId(uploadId)
              .partNumber(partNumber)
              .build(),
          requestBody);

      completedParts.add(CompletedPart.builder()
          .partNumber(partNumber)
          .eTag(uploadPartResponseFuture.join().eTag())
          .build());

      partNumber++;
    }

    inputStream.close();

    // Krok 4: Zakończ multipart upload
    CompletableFuture<CompleteMultipartUploadResponse> completeResponseFuture = s3AsyncClientMinio.completeMultipartUpload(
        CompleteMultipartUploadRequest.builder()
            .bucket(destinationBucket)
            .key(key)
            .uploadId(uploadId)
            .multipartUpload(CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build())
            .build());

    completeResponseFuture.whenComplete((resp, err) -> {
      if (resp != null) {
        System.out.println("Multipart upload completed successfully.");
      } else {
        System.err.println("Multipart upload failed: " + err.getMessage());
        // Można anulować upload w przypadku błędu
        s3AsyncClientMinio.abortMultipartUpload(AbortMultipartUploadRequest.builder()
            .bucket(destinationBucket)
            .key(key)
            .uploadId(uploadId)
            .build());
      }
    }).join();
  }
}

package pl.catalogic.demo.s3.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.catalogic.demo.s3.v2.model.ObjectVersionToTransfer;
import pl.catalogic.demo.s3.v2.model.S3StreamException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener.Context.BytesTransferred;

@Component
class S3DataTransferManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3DataTransferManager.class);
  private static final int DEFAULT_THREAD_COUNT = 20;

  private Integer numberOfThreads = 20;

  public List<CompletedUpload> processTransferBatch(
      List<ObjectVersionToTransfer> batch,
      S3AsyncClient sourceClient,
      S3AsyncClient destinationClient,
      String sourceBucketName,
      String destinationBucketName,
      S3TransferManager destinationTransferManager,
      ExecutorService executorService) {
    if (numberOfThreads <= 0 || numberOfThreads >= 500) {
      LOGGER.warn(
          "Invalid S3 transfer threads value: {}, switching to default value: {}",
          numberOfThreads,
          DEFAULT_THREAD_COUNT);
      numberOfThreads = DEFAULT_THREAD_COUNT;
    }
    var subBatchesFutures = new ArrayList<CompletedUpload>();
    for (var i = 0; i < batch.size(); i += numberOfThreads) {
      var subBatch = batch.subList(i, Math.min(i + numberOfThreads, batch.size()));
      var future =
          processSubBatchByNewThread(
              sourceClient,
              sourceBucketName,
              destinationBucketName,
              destinationTransferManager,
              executorService,
              subBatch);
      subBatchesFutures.addAll(future);
    }

    return subBatchesFutures;
  }

  private List<CompletedUpload> processSubBatchByNewThread(
      S3AsyncClient sourceClient,
      String sourceBucketName,
      String destinationBucketName,
      S3TransferManager destinationTransferManager,
      ExecutorService executorService,
      List<ObjectVersionToTransfer> subBatch) {
    var futures =
        subBatch.stream()
            .map(
                object ->
                    transferObjectByIdentifier(
                            sourceClient,
                            sourceBucketName,
                            destinationTransferManager,
                            destinationBucketName,
                            executorService)
                        .apply(object)
                        .thenApply(
                            getCompletedUploadAndInformTransferBatch(object)))
            .toList();
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(ignored -> futures.stream().map(CompletableFuture::join).toList())
        .exceptionally(
            ex -> {
              throw new S3StreamException(ex);
            })
        .join();
  }

  private Function<ObjectVersionToTransfer, CompletableFuture<CompletedUpload>>
      transferObjectByIdentifier(
          S3AsyncClient sourceClient,
          String sourceBucketName,
          S3TransferManager destinationTransferManager,
          String destinationBucket,
          ExecutorService executorService) {
    return objectVersionToTransfer ->
        transferObject(
            prepareGetRequestObject(objectVersionToTransfer, sourceBucketName),
            objectVersionToTransfer.size(),
            sourceClient,
            destinationBucket,
            executorService,
            destinationTransferManager);
  }

  private CompletableFuture<CompletedUpload> transferObject(
      GetObjectRequest objectToTransferRequest,
      long objectSize,
      S3AsyncClient sourceClient,
      String destBucket,
      ExecutorService executor,
      S3TransferManager s3TransferManager) {

    var downloadStreamFuture =
        CompletableFuture.supplyAsync(
            () -> getSourceDownloadStream(sourceClient, objectToTransferRequest), executor);

    return downloadStreamFuture.thenComposeAsync(
        sourceDownloadStream -> {
          var uploadFuture =
              s3TransferManager
                  .upload(
                      prepareUploadRequest(
                          destBucket,
                          executor,
                          sourceDownloadStream,
                          objectSize,
                          objectToTransferRequest))
                  .completionFuture();

          return handleStreamAfterUploadComplete(uploadFuture, sourceDownloadStream);
        },
        executor);
  }

  private CompletableFuture<CompletedUpload> handleStreamAfterUploadComplete(
      CompletableFuture<CompletedUpload> uploadFuture,
      ResponseInputStream<GetObjectResponse> sourceDownloadStream) {
    return uploadFuture.whenComplete(
        (result, error) -> {
          try {
            sourceDownloadStream.close();
            if (error != null) {
              throw new S3StreamException("Upload failed", error);
            }
          } catch (IOException e) {
            throw new S3StreamException("Error closing stream", e);
          }
        });
  }

  private ResponseInputStream<GetObjectResponse> getSourceDownloadStream(
      S3AsyncClient sourceClient, GetObjectRequest objectToTransferRequest) {
    return sourceClient
        .getObject(objectToTransferRequest, AsyncResponseTransformer.toBlockingInputStream())
        .join();
  }

  private Function<CompletedUpload, CompletedUpload> getCompletedUploadAndInformTransferBatch(
      ObjectVersionToTransfer object) {
    return transferResult -> {
      return transferResult;
    };
  }

  private Consumer<UploadRequest.Builder> prepareUploadRequest(
      String destBucket,
      ExecutorService executor,
      ResponseInputStream<GetObjectResponse> sourceDownloadStream,
      long objectSize,
      GetObjectRequest file) {
    return builder ->
        builder
            .requestBody(
                transferToDestinationRequestBody(executor, sourceDownloadStream, objectSize))
            .addTransferListener(
                new TransferListener() {
                  @Override
                  public void bytesTransferred(BytesTransferred context) {
                    LOGGER.trace(
                        "[{}] Transferred: {}, Bytes transferred: {}, Bytes left: {}",
                        context.request(),
                        context.progressSnapshot().ratioTransferred(),
                        context.progressSnapshot().transferredBytes(),
                        context.progressSnapshot().remainingBytes());
                  }
                })
            .putObjectRequest(transferToDestinationObjectRequest(file, destBucket));
  }

  private Consumer<PutObjectRequest.Builder> transferToDestinationObjectRequest(
      GetObjectRequest file, String destBucket) {
    return uploadRequest ->
        uploadRequest
            .key(file.key())
            .metadata(Map.of("catalogicEtag", UUID.randomUUID().toString()))
            .bucket(destBucket);
  }

  private AsyncRequestBody transferToDestinationRequestBody(
      ExecutorService executorService,
      ResponseInputStream<GetObjectResponse> sourceDownloadStream,
      long objectSize) {
    return AsyncRequestBody.fromInputStream(
        sourceDownloadStreamBuilder ->
            sourceDownloadStreamBuilder
                .inputStream(sourceDownloadStream)
                .contentLength(objectSize)
                .executor(executorService));
  }

  private GetObjectRequest prepareGetRequestObject(
      ObjectVersionToTransfer objectVersionToTransfer, String sourceBucket) {
    LOGGER.debug(
        "Getting object {}, version: {}",
        objectVersionToTransfer.key(),
        objectVersionToTransfer.versionId());
    return GetObjectRequest.builder()
        .bucket(sourceBucket)
        .key(objectVersionToTransfer.key())
        .versionId(objectVersionToTransfer.versionId())
        .build();
  }
}

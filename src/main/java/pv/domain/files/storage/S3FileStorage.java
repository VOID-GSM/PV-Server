package pv.domain.files.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pv.global.config.StorageProperties;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3FileStorage {
    public static final String TAGGING_HEADER = "x-amz-tagging";
    public static final String TEMP_TAGGING = "status=temp";

    private static final int DELETE_BATCH_SIZE = 1000;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    public PresignedUrl presignUpload(String objectKey, String contentType, long size, Duration ttl) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(size)
                .tagging(TEMP_TAGGING)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(r -> r
                .signatureDuration(ttl)
                .putObjectRequest(put));

        return new PresignedUrl(presigned.url().toString(), presigned.expiration());
    }

    public PresignedUrl presignDownload(String objectKey, String contentDisposition, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .responseContentDisposition(contentDisposition)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(r -> r
                .signatureDuration(ttl)
                .getObjectRequest(get));

        return new PresignedUrl(presigned.url().toString(), presigned.expiration());
    }

    public Optional<Long> findObjectSize(String objectKey) {
        try {
            HeadObjectResponse head = s3Client.headObject(r -> r
                    .bucket(properties.bucket())
                    .key(objectKey));
            return Optional.of(head.contentLength());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public boolean removeTempTag(String objectKey) {
        try {
            s3Client.deleteObjectTagging(r -> r
                    .bucket(properties.bucket())
                    .key(objectKey));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    public void deleteObjects(Collection<String> objectKeys) {
        List<ObjectIdentifier> ids = objectKeys.stream()
                .distinct()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();

        for (int from = 0; from < ids.size(); from += DELETE_BATCH_SIZE) {
            List<ObjectIdentifier> chunk = ids.subList(from, Math.min(from + DELETE_BATCH_SIZE, ids.size()));
            DeleteObjectsResponse response = s3Client.deleteObjects(r -> r
                    .bucket(properties.bucket())
                    .delete(d -> d.objects(chunk).quiet(true)));

            if (response.hasErrors() && !response.errors().isEmpty()) {
                response.errors().forEach(err ->
                        log.warn("S3 객체 삭제 실패 key={} code={} message={}", err.key(), err.code(), err.message()));
            }
        }
    }
}

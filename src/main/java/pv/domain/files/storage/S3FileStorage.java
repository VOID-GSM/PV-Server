package pv.domain.files.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pv.global.config.StorageProperties;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class S3FileStorage {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    public PresignedUrl presignUpload(String objectKey, String contentType, long size, Duration ttl) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(size)
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

}

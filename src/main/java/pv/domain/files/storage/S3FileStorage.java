package pv.domain.files.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pv.global.config.StorageProperties;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Component
@RequiredArgsConstructor
public class S3FileStorage {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;
}

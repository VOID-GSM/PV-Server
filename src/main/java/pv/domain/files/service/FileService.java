package pv.domain.files.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pv.domain.files.dto.FileDownloadResponse;
import pv.domain.files.dto.UploadUrlRequest;
import pv.domain.files.dto.UploadUrlResponse;
import pv.domain.files.entity.StoredFile;
import pv.domain.files.repository.FileRepository;
import pv.domain.files.storage.PresignedUrl;
import pv.domain.files.storage.S3FileStorage;
import pv.global.config.StorageProperties;
import pv.global.exception.FileException;
import pv.domain.files.dto.FileUploadCompleteRequest;
import pv.domain.files.dto.FileUploadCompleteResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-z0-9]{1,10}");
    private static final Set<String> INLINE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "application/pdf"
    );
    private final FileRepository fileRepository;
    private final S3FileStorage fileStorage;
    private final StorageProperties properties;

    @Transactional
    public UploadUrlResponse issueUploadUrl(Long memberId, UploadUrlRequest request) {
        validateUpload(request);
        String objectKey = generateObjectKey(memberId, request.fileName());
        fileRepository.save(StoredFile.pending(
                objectKey,
                request.fileName().strip(),
                request.contentType(),
                request.size(),
                memberId,
                Instant.now()));
        PresignedUrl presigned = fileStorage.presignUpload(
                objectKey, request.contentType(), request.size(), properties.uploadUrlTtl());
        return new UploadUrlResponse(
                objectKey,
                presigned.url(),
                Map.of("Content-Type", request.contentType(),
                        S3FileStorage.TAGGING_HEADER, S3FileStorage.TEMP_TAGGING),
                presigned.expiresAt());
    }

    @Transactional
    public FileUploadCompleteResponse register(Long memberId, FileUploadCompleteRequest request) {
        StoredFile file = fileRepository.findByObjectKey(request.objectKey())
                .orElseThrow(FileException::notFound);

        if (!file.isOwnedBy(memberId)) {
            throw FileException.forbidden();
        }
        if (file.isUploaded()) {
            return new FileUploadCompleteResponse(file.getId());
        }

        long actualSize = fileStorage.findObjectSize(file.getObjectKey())
                .orElseThrow(() -> FileException.badRequest("스토리지에 업로드가 완료되지 않았습니다."));

        if (actualSize != file.getSizeBytes()) {
            throw FileException.badRequest("신고한 파일 크기와 실제 업로드된 크기가 다릅니다.");
        }
        file.markUploaded(Instant.now());
        return new FileUploadCompleteResponse(file.getId());
    }

    public FileDownloadResponse getDownloadUrl(Long fileId) {
        StoredFile file = fileRepository.findById(fileId)
                .filter(StoredFile::isUploaded)
                .orElseThrow(FileException::notFound);

        PresignedUrl presigned = fileStorage.presignDownload(
                file.getObjectKey(),
                contentDisposition(file),
                properties.downloadUrlTtl());

        return new FileDownloadResponse(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSizeBytes(),
                presigned.url(),
                presigned.expiresAt());
    }

    @Transactional
    public void attach(Long memberId, Collection<String> objectKeys) {
        if (objectKeys.isEmpty()) {
            return;
        }
        Map<String, StoredFile> files = fileRepository.findAllByObjectKeyIn(objectKeys).stream()
                .collect(Collectors.toMap(StoredFile::getObjectKey, Function.identity()));

        for (String key : objectKeys) {
            StoredFile file = files.get(key);
            if (file == null) {
                throw FileException.badRequest("존재하지 않는 파일입니다: " + key);
            }
            if (!file.isOwnedBy(memberId)) {
                throw FileException.forbidden();
            }
        }

        Instant now = Instant.now();
        for (String key : objectKeys) {
            StoredFile file = files.get(key);
            if (!file.isUploaded()) {
                verifyUploaded(file);
                file.markUploaded(now);
            }

            if (!fileStorage.removeTempTag(key)) {
                throw FileException.badRequest(
                        "업로드되지 않았거나 만료된 파일입니다: " + key
                );
            }

            file.markAttached(now);
        }
    }

    @Transactional
    public void deleteByKeys(Collection<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }
        List<String> keys = objectKeys.stream().distinct().toList();
        fileRepository.deleteAllByObjectKeyIn(keys);
        runAfterCommit(() -> fileStorage.deleteObjects(keys));
    }

    @Transactional
    public int deleteUnattachedCreatedBefore(Instant threshold, int limit) {
        List<String> keys = fileRepository.lockUnattachedKeysCreatedBefore(threshold, limit);
        if (keys.isEmpty()) {
            return 0;
        }
        fileRepository.deleteAllByObjectKeyIn(keys);
        runAfterCommit(() -> fileStorage.deleteObjects(keys));
        return keys.size();
    }

    private void verifyUploaded(StoredFile file) {
        long actualSize = fileStorage.findObjectSize(file.getObjectKey())
                .orElseThrow(() -> FileException.badRequest("스토리지에 업로드가 완료되지 않았습니다."));
        if (actualSize != file.getSizeBytes()) {
            throw FileException.badRequest("신고한 파일 크기와 실제 업로드된 크기가 다릅니다.");
        }
    }

    private void runAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    task.run();
                } catch (Exception e) {
                    log.error("커밋 후 스토리지 파일 삭제 실패", e);
                }
            }
        });
    }

    private void validateUpload(UploadUrlRequest request) {
        if (request.size() > properties.maxSize().toBytes()) {
            throw FileException.badRequest(
                    "파일은 최대 " + properties.maxSize().toMegabytes() + "MB 까지 업로드할 수 있습니다.");
        }
        String baseType = baseContentType(request.contentType());
        if (!properties.allowedContentTypes().contains(baseType)) {
            throw FileException.badRequest("허용되지 않는 파일 형식입니다: " + baseType);
        }
    }

    private String generateObjectKey(Long memberId, String fileName) {
        return "uploads/" + memberId + "/" + UUID.randomUUID() + extractExtension(fileName);
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return SAFE_EXTENSION.matcher(ext).matches() ? "." + ext : "";
    }

    private String baseContentType(String contentType) {
        return contentType.split(";")[0].strip().toLowerCase(Locale.ROOT);
    }

    private String contentDisposition(StoredFile file) {
        String type = INLINE_TYPES.contains(baseContentType(file.getContentType())) ? "inline" : "attachment";
        String encoded = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        return type + "; filename*=UTF-8''" + encoded;
    }
}

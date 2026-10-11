package pv.domain.files.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {
    private static final DateTimeFormatter KEY_DATE = DateTimeFormatter.ofPattern("yyyy/MM");
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
        String objectKey = generateObjectKey(request.fileName());
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
                Map.of("Content-Type", request.contentType()),
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

    private String generateObjectKey(String fileName) {
        String datePath = LocalDate.now(ZoneOffset.UTC).format(KEY_DATE);
        return "files/" + datePath + "/" + UUID.randomUUID() + extractExtension(fileName);
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

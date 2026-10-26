package pv.domain.files.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "files",
        indexes = @Index(name = "idx_files_status_created_at", columnList = "status, created_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", nullable = false, unique = true, length = 500)
    private String objectKey;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 127)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FileStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "attached_at")
    private Instant attachedAt;

    private StoredFile(String objectKey, String originalName, String contentType,
                       Long sizeBytes, Long uploaderId, Instant now) {
        this.objectKey = Objects.requireNonNull(objectKey);
        this.originalName = Objects.requireNonNull(originalName);
        this.contentType = Objects.requireNonNull(contentType);
        this.sizeBytes = Objects.requireNonNull(sizeBytes);
        this.uploaderId = Objects.requireNonNull(uploaderId);
        this.status = FileStatus.PENDING;
        this.createdAt = now;
    }

    public static StoredFile pending(String objectKey, String originalName, String contentType,
                                     Long sizeBytes, Long uploaderId, Instant now) {
        return new StoredFile(objectKey, originalName, contentType, sizeBytes, uploaderId, now);
    }

    public void markUploaded(Instant now) {
        if (this.status == FileStatus.UPLOADED) {
            return;
        }
        if (this.status != FileStatus.PENDING) {
            throw new IllegalStateException(
                    "PENDING 상태에서만 업로드 완료 처리할 수 있습니다. 현재 상태 : " + this.status
            );
        }
        this.status = FileStatus.UPLOADED;
        this.uploadedAt = now;
    }

    public void markAttached(Instant now) {
        if (this.attachedAt == null) {
            this.attachedAt = now;
        }
    }

    public boolean isUploaded() {
        return this.status == FileStatus.UPLOADED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.uploaderId.equals(memberId);
    }
}

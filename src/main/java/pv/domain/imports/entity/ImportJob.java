package pv.domain.imports.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import java.time.Duration;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "import_job",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_import_job_member_hash",
                columnNames = {"member_id", "content_hash"}),
        indexes = @Index(name = "idx_import_job_member", columnList = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // ZIP 파일의 SHA-256 (중복 업로드 방지)
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportStatus status;

    @Column(length = 500)
    private String failReason;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ImportJob(Long memberId, String contentHash, String fileName) {
        this.memberId = Objects.requireNonNull(memberId);
        this.contentHash = Objects.requireNonNull(contentHash);
        this.fileName = Objects.requireNonNull(fileName);
        this.status = ImportStatus.PROCESSING;
    }

    // 이 시간 이상 PROCESSING이면 서버 재시작 등으로 멈춘 것으로 본다
    public static final Duration STALE_AFTER = Duration.ofMinutes(30);

    // 이 기간 동안 확정하지 않은 READY는 임시 문서와 함께 삭제한다
    public static final Duration READY_EXPIRE_AFTER = Duration.ofDays(7);

    public boolean isStale(LocalDateTime now) {
        return status == ImportStatus.PROCESSING
                && createdAt.isBefore(now.minus(STALE_AFTER));

    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void markReady() {
        requireStatus(ImportStatus.PROCESSING);
        this.status = ImportStatus.READY;
    }

    public void markFailed(String reason) {
        requireStatus(ImportStatus.PROCESSING);
        this.status = ImportStatus.FAILED;
        this.failReason = reason != null && reason.length() > 500 ? reason.substring(0, 500) : reason;
    }

    public void confirm() {
        requireStatus(ImportStatus.READY);
        this.status = ImportStatus.CONFIRMED;
    }

    private void requireStatus(ImportStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException("잘못된 상태 전이입니다: " + this.status + " -> " + expected);
        }
    }
}
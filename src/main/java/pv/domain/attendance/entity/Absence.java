package pv.domain.attendance.entity;

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
import org.springframework.http.HttpStatus;
import pv.global.exception.CustomException;

import java.time.LocalDateTime;

@Entity
@Table(name = "absence", indexes = @Index(columnList = "activity_id, status"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Absence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessionId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AbsenceStatus status = AbsenceStatus.REQUESTED;

    @Column(length = 100)
    private String rejectComment;

    private Long reviewerId;
    private LocalDateTime reviewedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Absence(Long sessionId, Long activityId, Long memberId, String reason) {
        this.sessionId = sessionId;
        this.activityId = activityId;
        this.memberId = memberId;
        this.reason = reason;
    }

    public void approve(Long reviewerId) {
        review(AbsenceStatus.APPROVED, reviewerId, null);
    }

    public void review(AbsenceStatus next, Long reviewerId, String comment) {
        if (status != AbsenceStatus.REQUESTED) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 처리된 불참 요청입니다.");
        }
        this.status = next;
        this.reviewerId = reviewerId;
        this.rejectComment = comment;
        this.reviewedAt = LocalDateTime.now();
    }
}

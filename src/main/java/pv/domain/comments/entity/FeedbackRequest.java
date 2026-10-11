package pv.domain.comments.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.http.HttpStatus;
import pv.global.exception.CustomException;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_requests", indexes = @Index(name = "idx_feedback_receiver_pending", columnList = "receiver_id, completed_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private Long requesterId;

    @Column(nullable = false)
    private Long receiverId;

    @Column(length = 500)
    private String message;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public static FeedbackRequest create(TargetType targetType, Long targetId, Long requesterId,
                                         Long receiverId, String message) {
        if (requesterId.equals(receiverId)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "자기 자신에게 요청할 수 없습니다.");
        }
        FeedbackRequest r = new FeedbackRequest();
        r.targetType = targetType;
        r.targetId = targetId;
        r.requesterId = requesterId;
        r.receiverId = receiverId;
        r.message = message;
        return r;
    }

    public void complete() {
        if (completedAt == null) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 완료된 피드백 요청입니다.");
        }
        completedAt = LocalDateTime.now();
    }

    public boolean isReceiver(Long memberId) {
        return receiverId.equals(memberId);
    }
}

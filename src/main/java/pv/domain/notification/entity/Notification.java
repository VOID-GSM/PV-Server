package pv.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 알림 row는 대상자 수만큼 한 번에 만들어지므로 NotificationRepository.insertForMembers(네이티브 insert-select)로 저장한다.
// 목록은 (member_id, id) 인덱스로 커서 조회한다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_member_id", columnList = "member_id, id"),
        @Index(name = "idx_notification_target", columnList = "notification_type, target_id")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    // type에 따라 가리키는 대상이 달라진다. (NOTICE -> 공지 ID)
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isRead() {
        return readAt != null;
    }
}

package pv.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// row가 없으면 해당 종류는 수신(기본값)으로 본다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_setting", uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_setting_member_type",
        columnNames = {"member_id", "notification_type"}))
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private boolean enabled;

    public NotificationSetting(Long memberId, NotificationType type, boolean enabled) {
        this.memberId = memberId;
        this.type = type;
        this.enabled = enabled;
    }

    public void changeEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

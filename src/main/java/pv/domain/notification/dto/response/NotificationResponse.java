package pv.domain.notification.dto.response;

import pv.domain.notification.entity.Notification;
import pv.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        Long targetId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getTargetId(), n.isRead(), n.getCreatedAt());
    }
}

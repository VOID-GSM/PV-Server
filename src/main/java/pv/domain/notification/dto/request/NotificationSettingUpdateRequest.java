package pv.domain.notification.dto.request;

import jakarta.validation.constraints.NotNull;
import pv.domain.notification.entity.NotificationType;

public record NotificationSettingUpdateRequest(
        @NotNull(message = "알림 종류는 필수입니다.") NotificationType type,
        @NotNull(message = "수신 여부는 필수입니다.") Boolean enabled
) {
}

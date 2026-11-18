
package pv.domain.notification.dto.response;

import pv.domain.notification.entity.NotificationType;

import java.util.List;

public record NotificationSettingsResponse(
        List<SettingItem> settings
) {
    public record SettingItem(
            NotificationType type,
            boolean enabled
    ) {
    }
}

package pv.domain.notification.dto.response;

import java.util.List;

// nextCursor가 null이면 마지막 페이지. 다음 요청은 ?cursor={nextCursor}
public record NotificationListResponse(
        List<NotificationResponse> items,
        Long nextCursor
) {
}

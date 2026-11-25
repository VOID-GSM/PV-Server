package pv.domain.notice.dto.response;

import pv.domain.notice.dto.query.NoticeSummaryRow;
import pv.domain.notice.entity.NoticeScope;

import java.time.LocalDateTime;

public record NoticeSummaryResponse(
        Long id,
        String title,
        NoticeScope scope,
        Long activityId,   // 활동 공지가 아니면 null
        Long authorId,
        String authorName,
        LocalDateTime createdAt,
        boolean read
) {
    public static NoticeSummaryResponse from(NoticeSummaryRow row) {
        return new NoticeSummaryResponse(
                row.id(), row.title(), row.scope(), row.activityId(), row.authorId(), row.authorName(),
                row.createdAt(), row.readAt() != null);
    }
}

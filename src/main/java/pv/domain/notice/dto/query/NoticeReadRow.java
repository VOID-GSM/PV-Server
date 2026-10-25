package pv.domain.notice.dto.query;

import java.time.LocalDateTime;

public record NoticeReadRow(
        Long memberId,
        String name,
        LocalDateTime readAt
) {
}

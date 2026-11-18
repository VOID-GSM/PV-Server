package pv.domain.notice.dto.query;

import pv.domain.notice.entity.NoticeScope;

import java.time.LocalDateTime;

// 공지 목록 한 줄. 수신자 + 공지 + 작성자 이름을 한 번의 쿼리로 가져온다.
public record NoticeSummaryRow(
        Long id,
        String title,
        NoticeScope scope,
        Long authorId,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}

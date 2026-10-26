package pv.domain.notice.dto.response;

import pv.domain.notice.dto.query.NoticeReadRow;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeReadStatusResponse(
        int totalCount,
        int readCount,
        List<Item> items
) {
    public record Item(
            Long memberId,
            String name,
            boolean read,
            LocalDateTime readAt
    ) {
        static Item from(NoticeReadRow row) {
            return new Item(row.memberId(), row.name(), row.readAt() != null, row.readAt());
        }
    }

    public static NoticeReadStatusResponse from(List<NoticeReadRow> rows) {
        List<Item> items = rows.stream().map(Item::from).toList();
        int readCount = (int) items.stream().filter(Item::read).count();
        return new NoticeReadStatusResponse(items.size(), readCount, items);
    }
}

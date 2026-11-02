package pv.domain.calendar.dto;

import pv.domain.calendar.entity.CalendarEvent;
import pv.domain.calendar.entity.EventType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record EventResponse(
        Long id,
        EventType type,
        String title,
        String memo,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean allDay,
        Long boardId,
        Long activityId
) {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static EventResponse from(CalendarEvent e) {
        return new EventResponse(e.getId(), e.getType(), e.getTitle(), e.getMemo(),
                LocalDateTime.ofInstant(e.getStartsAt(), KST),
                LocalDateTime.ofInstant(e.getEndsAt(), KST),
                e.isAllDay(), e.getBoardId(), e.getActivityId());
    }
}

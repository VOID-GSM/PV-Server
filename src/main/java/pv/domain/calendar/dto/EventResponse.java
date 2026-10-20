package pv.domain.calendar.dto;

import pv.domain.calendar.entity.CalendarEvent;
import pv.domain.calendar.entity.EventType;

import java.time.Instant;

public record EventResponse(
        Long id,
        EventType type,
        String title,
        String memo,
        Instant startsAt,
        Instant endsAt,
        boolean allDay,
        Long boardId,
        Long activityId
) {
    public static EventResponse from(CalendarEvent e) {
        return new EventResponse(
                e.getId(),
                e.getType(),
                e.getTitle(),
                e.getMemo(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.isAllDay(),
                e.getBoardId(),
                e.getActivityId());
    }
}

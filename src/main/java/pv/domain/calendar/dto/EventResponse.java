package pv.domain.calendar.dto;

import pv.domain.calendar.entity.CalendarEvent;
import pv.domain.calendar.entity.EventType;
import pv.domain.calendar.port.CalendarDeadlinePort;
import pv.domain.calendar.port.CalendarSessionPort;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static pv.global.exception.TimeZones.KST;

public record EventResponse(
        Long id,
        EventType type,
        String title,
        String memo,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean allDay,
        Long boardId,
        Long activityId,
        Long assigneeId
) {
    private static final Duration DEFAULT_SESSION_DURATION = Duration.ofHours(1);

    public static EventResponse from(CalendarEvent e) {
        return new EventResponse(e.getId(), e.getType(), e.getTitle(), e.getMemo(),
                LocalDateTime.ofInstant(e.getStartsAt(), KST),
                LocalDateTime.ofInstant(e.getEndsAt(), KST),
                e.isAllDay(), e.getBoardId(), e.getActivityId(), null);
    }

    public static EventResponse from(CalendarDeadlinePort.DeadlineView d) {
        return new EventResponse(d.id(), EventType.DEADLINE, d.name(), null,
                d.date().atStartOfDay(),
                d.date().atTime(LocalTime.of(23, 59, 59)),
                true, d.boardId(), null, d.assigneeId());
    }

    public static EventResponse from(CalendarSessionPort.SessionView s) {
        String title = s.activityName() + " " + s.sessionNo() + "회차";
        boolean allDay = s.time() == null;
        LocalDateTime startsAt = allDay ? s.date().atStartOfDay() : s.date().atTime(s.time());
        LocalDateTime endsAt = allDay
                ? s.date().atTime(LocalTime.of(23, 59, 59))
                : startsAt.plus(DEFAULT_SESSION_DURATION);
        return new EventResponse(s.id(), EventType.ACTIVITY, title, s.topic(),
                startsAt, endsAt, allDay, null, s.activityId(), null);
    }
}

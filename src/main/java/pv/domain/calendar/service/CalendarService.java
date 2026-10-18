package pv.domain.calendar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.calendar.dto.ConflictResponse;
import pv.domain.calendar.dto.EventCreateRequest;
import pv.domain.calendar.dto.EventResponse;
import pv.domain.calendar.dto.EventUpdateRequest;
import pv.domain.calendar.entity.CalendarEvent;
import pv.domain.calendar.entity.EventType;
import pv.domain.calendar.port.CalendarMembershipPort;
import pv.domain.calendar.repository.CalendarEventRepository;
import org.springframework.http.HttpStatus;
import pv.global.exception.CustomException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final long MAX_RANGE_DAYS = 92;
    private static final List<Long> NONE = List.of(-1L);

    private final CalendarEventRepository eventRepository;
    private final CalendarMembershipPort membership;

    public List<EventResponse> getCalendar(Long memberId, LocalDate from, LocalDate to,
                                           Set<Long> boardFilter, boolean mine) {
        validateRange(from, to);

        Set<Long> boards = membership.boardIdsOf(memberId);
        if (boardFilter != null && !boardFilter.isEmpty()) {
            boards = intersect(boards, boardFilter);
        }
        Set<Long> activities = mine
                ? membership.activityIdsOf(memberId)
                : membership.teamActivityIdsOf(memberId);

        return eventRepository.findCalendar(
                        memberId, startOf(from), startOf(to.plusDays(1)),
                        orNone(boards), orNone(activities), EventType.PERSONAL)
                .stream()
                .map(EventResponse::from)
                .toList();
    }

    public List<ConflictResponse> getConflicts(Long memberId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        Set<Long> boards = membership.boardIdsOf(memberId);
        if (boards.isEmpty()) {
            return List.of();
        }

        LocalDate weekFrom = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekTo = to.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return eventRepository.findDeadlines(boards, startOf(weekFrom), startOf(weekTo.plusDays(1)), EventType.DEADLINE)
                .stream()
                .collect(groupingBy(e -> weekStartOf(e.getStartsAt()), TreeMap::new, toList()))
                .entrySet().stream()
                .filter(en -> en.getValue().size() >= 2)
                .map(en -> new ConflictResponse(
                        en.getKey(),
                        en.getValue().stream().map(EventResponse::from).toList()))
                .toList();
    }

    @Transactional
    public EventResponse update(Long memberId, Long eventId, EventUpdateRequest req) {
        CalendarEvent event = findOwned(memberId, eventId);
        event.update(req.title(), req.memo(), req.startsAt(), req.endsAt(), req.allDay());
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse create(Long memberId, EventCreateRequest req) {
        CalendarEvent event = CalendarEvent.personal(
                memberId, req.title(), req.memo(), req.startsAt(), req.endsAt(), req.allDay());
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public void delete(Long memberId, Long eventId) {
        eventRepository.delete(findOwned(memberId, eventId));
    }

    private CalendarEvent findOwned(Long memberId, Long eventId) {
        return eventRepository.findById(eventId)
                .filter(e -> e.isPersonalOf(memberId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."));
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "to는 from 이후여야 합니다.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "조회 기간은 최대 " + MAX_RANGE_DAYS + "일입니다.");
        }
    }

    private static Instant startOf(LocalDate date) {
        return date.atStartOfDay(KST).toInstant();
    }

    private static LocalDate weekStartOf(Instant instant) {
        return instant.atZone(KST).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static Collection<Long> orNone(Set<Long> ids) {
        return ids == null || ids.isEmpty() ? NONE : ids;
    }

    private static Set<Long> intersect(Set<Long> a, Set<Long> b) {
        Set<Long> r = new HashSet<>(a);
        r.retainAll(b);
        return r;
    }
}

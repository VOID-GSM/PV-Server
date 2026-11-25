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
import pv.domain.calendar.port.CalendarDeadlinePort;
import pv.domain.calendar.port.CalendarDeadlinePort.DeadlineView;
import pv.domain.calendar.port.CalendarSessionPort;
import pv.domain.calendar.repository.CalendarEventRepository;
import org.springframework.http.HttpStatus;
import pv.global.exception.CustomException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static pv.global.exception.TimeZones.KST;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {
    private static final long MAX_RANGE_DAYS = 92;
    private static final List<Long> NONE = List.of(-1L);

    private final CalendarEventRepository eventRepository;
    private final CalendarMembershipPort membership;
    private final CalendarDeadlinePort deadlinePort;
    private final CalendarSessionPort sessionPort;
    private final Clock clock;

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

        List<EventResponse> events = new ArrayList<>(eventRepository.findCalendar(
                        memberId, startOf(from), startOf(to.plusDays(1)),
                        orNone(boards), orNone(activities), EventType.PERSONAL)
                .stream()
                .filter(e -> !mine || !e.isMeeting() || e.getAssigneeIds().contains(memberId))   // ← 추가
                .map(EventResponse::from)
                .toList());

        deadlinePort.findDeadlines(boards, from, to).stream()
                .filter(d -> !mine || d.assigneeIds().contains(memberId))
                .map(EventResponse::from)
                .forEach(events::add);

        sessionPort.findSessions(activities, from, to).stream()
                .map(EventResponse::from)
                .forEach(events::add);

        events.sort(comparing(EventResponse::startsAt));
        return events;
    }

    public List<ConflictResponse> getConflicts(Long memberId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        Set<Long> boards = membership.boardIdsOf(memberId);
        if (boards.isEmpty()) {
            return List.of();
        }

        LocalDate weekFrom = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekTo = to.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        record Key(LocalDate weekStart, Long assigneeId) {}

        Map<Key, List<DeadlineView>> grouped = deadlinePort.findDeadlines(boards, weekFrom, weekTo).stream()
                .flatMap(d -> d.assigneeIds().stream()
                        .map(a -> Map.entry(new Key(weekStartOf(d.date()), a), d)))
                .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));

        return grouped.entrySet().stream()
                .filter(en -> en.getValue().size() >= 2)
                .sorted(comparing((Map.Entry<Key, List<DeadlineView>> en) -> en.getKey().weekStart())
                        .thenComparing(en -> en.getKey().assigneeId()))
                .map(en -> new ConflictResponse(
                        en.getKey().weekStart(),
                        en.getKey().assigneeId(),
                        en.getValue().stream().map(EventResponse::from).toList()))
                .toList();
    }

    @Transactional
    public EventResponse update(Long memberId, Long eventId, EventUpdateRequest req) {
        CalendarEvent event = findEditable(memberId, eventId);
        if (req.completed() != null && event.isMeeting()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "회의 일정은 완료 처리할 수 없습니다.");
        }
        if (req.assigneeIds() != null) {
            validateAssignees(memberId, event.getType(), req.assigneeIds());
        }
        event.update(req.title(), req.memo(), toInstant(req.startsAt()), toInstant(req.endsAt()), req.allDay());
        event.changeAssignees(req.assigneeIds());
        if (req.completed() != null) {
            event.changeCompletion(req.completed(), Instant.now(clock));
        }
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse create(Long memberId, EventCreateRequest req) {
        EventType type = req.type() == null ? EventType.PERSONAL : req.type();
        Instant startsAt = toInstant(req.startsAt());
        Instant endsAt = toInstant(req.endsAt());

        CalendarEvent event = switch (type) {
            case PERSONAL -> {
                if (req.boardId() != null) {
                    throw new CustomException(HttpStatus.BAD_REQUEST, "개인 일정에는 보드를 지정할 수 없습니다.");
                }
                validateAssignees(memberId, EventType.PERSONAL, req.assigneeIds());
                yield CalendarEvent.personal(memberId, req.title(), req.memo(), startsAt, endsAt, req.allDay());
            }
            case MEETING -> {
                if (req.boardId() == null) {
                    throw new CustomException(HttpStatus.BAD_REQUEST, "회의 일정은 보드가 필수입니다.");
                }
                requireBoardManager(memberId, req.boardId());
                validateAssignees(memberId, EventType.MEETING, req.assigneeIds());
                yield CalendarEvent.meeting(req.boardId(), memberId, req.title(), req.memo(),
                        startsAt, endsAt, req.allDay(), req.assigneeIds());
            }
            default -> throw new CustomException(HttpStatus.BAD_REQUEST, "이 API로 만들 수 없는 일정 유형입니다.");
        };
        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public void delete(Long memberId, Long eventId) {
        eventRepository.delete(findEditable(memberId, eventId));
    }

    private CalendarEvent findEditable(Long memberId, Long eventId) {
        return eventRepository.findById(eventId)
                .filter(e -> e.isPersonalOf(memberId) || (e.isMeeting() && canManageBoard(memberId, e.getBoardId())))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."));
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

    private static Instant toInstant(LocalDateTime kst) {
        return kst == null ? null : kst.atZone(KST).toInstant();
    }

    private static LocalDate weekStartOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static Collection<Long> orNone(Set<Long> ids) {
        return ids == null || ids.isEmpty() ? NONE : ids;
    }

    private static Set<Long> intersect(Set<Long> a, Set<Long> b) {
        Set<Long> r = new HashSet<>(a);
        r.retainAll(b);
        return r;
    }

    private void requireBoardManager(Long memberId, Long boardId) {
        if (!membership.boardIdsOf(memberId).contains(boardId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "존재하지 않는 보드입니다.");
        }
        if (!membership.isLeader(memberId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "보드 회의는 부장만 만들 수 있습니다.");
        }
    }

    private void validateAssignees(Long memberId, EventType type, Collection<Long> assigneeIds) {
        if (assigneeIds == null || assigneeIds.isEmpty()) return;
        if (type != EventType.MEETING) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "담당자는 회의 일정에만 지정할 수 있습니다.");
        }
        if (!membership.areJoinedTeammates(memberId, assigneeIds)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "존재하지 않는 담당자입니다.");
        }
    }

    private boolean canManageBoard(Long memberId, Long boardId) {
        return membership.isLeader(memberId) && membership.boardIdsOf(memberId).contains(boardId);
    }
}

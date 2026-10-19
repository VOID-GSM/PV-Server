package pv.domain.home.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.activity.repository.ActivityRepository;
import pv.domain.activity.repository.ActivitySessionRepository;
import pv.domain.attendance.dto.StatusCount;
import pv.domain.attendance.dto.response.AbsenceResponse;
import pv.domain.attendance.entity.AttendanceStatus;
import pv.domain.attendance.service.AbsenceService;
import pv.domain.home.dto.DashboardResponse;
import pv.domain.home.dto.HomeTodosResponse;
import pv.domain.home.dto.MemberDashboardResponse;
import pv.domain.home.dto.TodoItemResponse;
import pv.domain.home.repository.HomeQueryRepository;
import pv.domain.home.repository.IdCountRow;
import pv.domain.home.type.TodoType;
import pv.domain.member.entity.Member;
import pv.domain.member.repository.MemberRepository;
import pv.global.exception.CustomException;


import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int TOPIC_VOTE_DEADLINE_HOUR = 15;
    private static final int OVERLOAD_DEADLINE_THRESHOLD = 3;

    private final Clock clock;
    private final HomeQueryRepository homeQueryRepository;
    private final AbsenceService absenceService;
    private final ActivityRepository activityRepository;
    private final ActivitySessionRepository sessionRepository;
    private final MemberRepository memberRepository;

    public HomeTodosResponse getTodos(Long memberId) {
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), KST);
        LocalDate today = now.toLocalDate();

        List<TodoItemResponse> items = new ArrayList<>();
        items.addAll(topicInputTodos(memberId, today, now));
        items.addAll(voteTodos(memberId, today, now));
        items.addAll(absenceTodos(memberId, now));

        Map<TodoType, Long> counts = new EnumMap<>(TodoType.class);
        for (TodoType type : TodoType.values()) {
            counts.put(type, 0L);
        }
        items.forEach(item -> counts.merge(item.type(), 1L, Long::sum));

        return new HomeTodosResponse(today, items.size(), counts, items);
    }


    private List<TodoItemResponse> topicInputTodos(Long memberId, LocalDate today, OffsetDateTime now) {
        return homeQueryRepository.findTopicInputCandidates(memberId, today).stream()
                .filter(r -> !r.sessionDate().isAfter(today.plusDays(r.topicOpenDaysBefore())))
                .map(r -> TodoItemResponse.of(
                        TodoType.TOPIC_INPUT,
                        r.sessionId(),
                        r.activityName(),
                        r.sessionDate() + " 회차 주제를 입력해 주세요.",
                        endOfDay(r.sessionDate()),
                        now))
                .toList();
    }

    private List<TodoItemResponse> voteTodos(Long memberId, LocalDate today, OffsetDateTime now) {
        LocalDateTime nowLocal = now.toLocalDateTime();
        return homeQueryRepository.findVoteCandidates(memberId, today).stream()
                .filter(r -> {
                    LocalDateTime openAt = r.sessionDate().minusDays(r.topicOpenDaysBefore()).atStartOfDay();
                    LocalDateTime deadline = r.sessionDate().atTime(TOPIC_VOTE_DEADLINE_HOUR, 0);
                    return !nowLocal.isBefore(openAt) && nowLocal.isBefore(deadline);
                })
                .map(r -> TodoItemResponse.of(
                        TodoType.TOPIC_VOTE,
                        r.sessionId(),
                        r.activityName(),
                        r.sessionDate() + " 회차 주제 투표",
                        r.sessionDate().atTime(TOPIC_VOTE_DEADLINE_HOUR, 0).atZone(KST).toOffsetDateTime(),
                        now))
                .toList();
    }

    private List<TodoItemResponse> absenceTodos(Long memberId, OffsetDateTime now) {
        List<AbsenceResponse> pending = absenceService.getPending(memberId).stream()
                .filter(a -> !a.memberId().equals(memberId))   // 본인 신청은 제외
                .toList();
        if (pending.isEmpty()) {
            return List.of();
        }

        Map<Long, String> activityNames = new HashMap<>();
        activityRepository.findAllById(ids(pending, AbsenceResponse::activityId))
                .forEach(a -> activityNames.put(a.getId(), a.getName()));

        Map<Long, LocalDate> sessionDates = new HashMap<>();
        sessionRepository.findAllById(ids(pending, AbsenceResponse::sessionId))
                .forEach(s -> sessionDates.put(s.getId(), s.getSessionDate()));

        // 이름이 null일 수 있어 Collectors.toMap 대신 직접 담음
        Map<Long, String> memberNames = new HashMap<>();
        memberRepository.findAllById(ids(pending, AbsenceResponse::memberId))
                .forEach(m -> memberNames.put(m.getId(), m.getName()));

        return pending.stream()
                .map(a -> {
                    LocalDate sessionDate = sessionDates.get(a.sessionId());
                    return TodoItemResponse.of(
                            TodoType.ABSENCE_APPROVE,
                            a.id(),
                            activityNames.get(a.activityId()),
                            memberNames.get(a.memberId()) + " · " + sessionDate + " · " + a.reason(),
                            sessionDate == null ? null : endOfDay(sessionDate),
                            now);
                })
                .toList();
    }

    public DashboardResponse getDashboard(Long memberId) {
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        if (!requester.isLeader()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "부장만 조회할 수 있습니다.");
        }
        Long teamId = requester.getTeamId();

        LocalDate today = OffsetDateTime.ofInstant(clock.instant(), KST).toLocalDate();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        Map<Long, Long> boardCounts = toCountMap(homeQueryRepository.countActiveBoardsByAssignee(teamId));
        Map<Long, Long> deadlineCounts = toCountMap(
                homeQueryRepository.countWeeklyDeadlinesByAssignee(teamId, weekStart, weekEnd));

        Map<Long, EnumMap<AttendanceStatus, Long>> attendance = new HashMap<>();
        for (StatusCount c : homeQueryRepository.countAttendanceByMember(teamId)) {
            attendance.computeIfAbsent(c.groupId(), k -> new EnumMap<>(AttendanceStatus.class))
                    .put(c.status(), c.count());
        }

        List<MemberDashboardResponse> members = homeQueryRepository.findTeamMembers(teamId).stream()
                .map(m -> {
                    long weekly = deadlineCounts.getOrDefault(m.memberId(), 0L);
                    EnumMap<AttendanceStatus, Long> att =
                            attendance.getOrDefault(m.memberId(), new EnumMap<>(AttendanceStatus.class));
                    long present = att.getOrDefault(AttendanceStatus.PRESENT, 0L);
                    long excused = att.getOrDefault(AttendanceStatus.EXCUSED, 0L);
                    long absent = att.getOrDefault(AttendanceStatus.ABSENT, 0L);

                    return new MemberDashboardResponse(
                            m.memberId(),
                            m.name(),
                            m.generation(),
                            boardCounts.getOrDefault(m.memberId(), 0L),
                            weekly,
                            weekly >= OVERLOAD_DEADLINE_THRESHOLD,
                            present,
                            excused,
                            absent,
                            rate(present, present + excused + absent),
                            0L,     // 과제 도메인 구현 전
                            0L,
                            null);
                })
                .toList();

        int overloaded = (int) members.stream().filter(MemberDashboardResponse::overloaded).count();
        return new DashboardResponse(weekStart, weekEnd, overloaded, members);
    }

    private static OffsetDateTime endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(KST).toOffsetDateTime();
    }

    private static Set<Long> ids(List<AbsenceResponse> list, Function<AbsenceResponse, Long> getter) {
        return list.stream().map(getter).collect(Collectors.toSet());
    }

    private static Map<Long, Long> toCountMap(List<IdCountRow> rows) {
        Map<Long, Long> map = new HashMap<>();
        rows.forEach(r -> map.put(r.id(), r.count()));
        return map;
    }

    private static Double rate(long numerator, long denominator) {
        return denominator == 0 ? null : Math.round(numerator * 1000.0 / denominator) / 10.0;
    }
}
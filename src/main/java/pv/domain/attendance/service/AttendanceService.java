package pv.domain.attendance.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.activity.repository.ActivityMemberRepository;
import pv.domain.attendance.dto.StatusCount;
import pv.domain.attendance.dto.request.BulkAttendanceRequest;
import pv.domain.attendance.dto.request.CheckInRequest;
import pv.domain.attendance.dto.response.AttendanceResponse;
import pv.domain.attendance.dto.response.AttendanceStatsResponse;
import pv.domain.attendance.entity.Attendance;
import pv.domain.attendance.entity.AttendanceSession;
import pv.domain.attendance.entity.AttendanceStatus;
import pv.domain.attendance.repository.AttendanceSessionRepository;
import pv.global.exception.CustomException;
import pv.domain.attendance.repository.AttendanceRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceAuthorizer authorizer;
    private final ActivityMemberRepository activityMemberRepository;

    @Transactional
    public void openSession(Long sessionId, Long activityId, String checkInCode,
                            LocalDateTime opensAt, LocalDateTime closesAt) {
        if (sessionRepository.existsById(sessionId)) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 출석이 생성된 회차입니다.");
        }
        sessionRepository.save(new AttendanceSession(sessionId, activityId, checkInCode, opensAt, closesAt));

        List<Attendance> attendances = activityMemberRepository.findMemberIdsByActivityId(activityId).stream()
                .map(memberId -> new Attendance(sessionId, activityId, memberId))
                .toList();
        attendanceRepository.saveAll(attendances);
    }

    @Transactional
    public AttendanceResponse checkIn(Long sessionId, Long memberId, CheckInRequest req) {
        AttendanceSession session = getSession(sessionId);
        LocalDateTime now = LocalDateTime.now();

        if (!session.isCheckInOpen(now)) throw new CustomException(HttpStatus.BAD_REQUEST, "체크인 가능 시간이 아닙니다.");
        if (!session.matchesCode(req.code())) throw new CustomException(HttpStatus.BAD_REQUEST, "출석 코드가 올바르지 않습니다.");

        Attendance a = attendanceRepository.findBySessionIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.FORBIDDEN, "이 회차의 참석 대상이 아닙니다."));
        a.checkIn(now);
        return AttendanceResponse.from(a);
    }

    @Transactional
    public List<AttendanceResponse> bulkUpdate(Long sessionId, Long requesterId, BulkAttendanceRequest req) {
        AttendanceSession session = getSession(sessionId);
        requireLeader(session.getActivityId(), requesterId);
        Map<Long, AttendanceStatus> changes = req.items().stream()
                .collect(Collectors.toMap(BulkAttendanceRequest.Item::memberId,
                        BulkAttendanceRequest.Item::status, (x, y) -> y));
        List<Attendance> targets = attendanceRepository.findAllBySessionIdAndMemberIdIn(sessionId, changes.keySet());
        if (targets.size() != changes.size()) throw new CustomException(HttpStatus.BAD_REQUEST, "참석 대상이 아닌 멤버가 포함되어 있습니다.");
        targets.forEach(a -> a.changeStatus(changes.get(a.getMemberId()), LocalDateTime.now()));
        return targets.stream().map(AttendanceResponse::from).toList();
    }

    public List<AttendanceResponse> getSessionAttendance(Long sessionId, Long requesterId) {
        AttendanceSession session = getSession(sessionId);
        Long activityId = session.getActivityId();
        if (!authorizer.isParticipant(activityId, requesterId)
        && !authorizer.isLeader(activityId, requesterId)
        && !authorizer.isManager(requesterId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "조회 권한이 없습니다.");
        }
        return attendanceRepository.findAllBySessionId(sessionId).stream()
                .map(AttendanceResponse::from).toList();
    }

    public List<AttendanceStatsResponse> getMemberStats(Long memberId, Long requesterId) {
        List<StatusCount> counts = attendanceRepository.countByMemberGroupByActivity(memberId);
        if (requesterId.equals(memberId) || authorizer.isManager(requesterId)) {
            return AttendanceStatsResponse.from(counts);
        }
        Set<Long> leading = new HashSet<>(authorizer.leadingActivityIds(requesterId));
        if (leading.isEmpty()) throw new CustomException(HttpStatus.FORBIDDEN, "조회 권한이 없습니다.");
        return AttendanceStatsResponse.from(counts.stream().filter(c -> leading.contains(c.groupId())).toList());
    }

    public List<AttendanceStatsResponse> getActivityStats(Long activityId, Long requesterId) {
        requireLeader(activityId, requesterId);
        return AttendanceStatsResponse.from(attendanceRepository.countByActivityGroupByMember(activityId));
    }

    @Transactional
    public int closeSession(Long sessionId) {
        AttendanceSession session = getSession(sessionId);
        session.close();
        return attendanceRepository.bulkChangeStatus(sessionId, AttendanceStatus.PENDING, AttendanceStatus.ABSENT);
    }

    void requireLeader(Long activityId, Long memberId) {
        if (!authorizer.isLeader(activityId, memberId) && !authorizer.isManager(memberId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "스터디장 또는 부장만 가능합니다.");
        }
    }

    private AttendanceSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회차를 찾을 수 없습니다."));
    }
}

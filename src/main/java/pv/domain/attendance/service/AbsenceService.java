package pv.domain.attendance.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.attendance.dto.request.AbsenceRejectRequest;
import pv.domain.attendance.dto.request.AbsenceRequest;
import pv.domain.attendance.dto.response.AbsenceResponse;
import pv.domain.attendance.entity.Absence;
import pv.domain.attendance.entity.AbsenceStatus;
import pv.domain.attendance.entity.Attendance;
import pv.domain.attendance.entity.AttendanceStatus;
import pv.domain.attendance.repository.AbsenceRepository;
import pv.domain.attendance.repository.AttendanceRepository;
import pv.global.exception.CustomException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbsenceService {
    private final AbsenceRepository absenceRepository;
    private final AttendanceAuthorizer authorizer;
    private final AttendanceService attendanceService;
    private final AttendanceRepository attendanceRepository;

    @Transactional
    public AbsenceResponse request(Long memberId, AbsenceRequest req) {
        Attendance attendance = attendanceRepository.findWithLockBySessionIdAndMemberId(req.sessionId(), memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.FORBIDDEN, "이 회차 참석 대상이 아닙니다."));

        if (attendance.getStatus() == AttendanceStatus.PRESENT) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 출석한 회차입니다.");
        }
        if (absenceRepository.existsBySessionIdAndMemberIdAndStatusIn(
                req.sessionId(), memberId, List.of(AbsenceStatus.REQUESTED, AbsenceStatus.APPROVED))) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 불참 요청이 있습니다.");
        }

        Absence absence = absenceRepository.save(
                new Absence(req.sessionId(), attendance.getActivityId(), memberId, req.reason()));
        return AbsenceResponse.from(absence);
    }

    public List<AbsenceResponse> getPending(Long requesterId) {
        List<Absence> list = authorizer.isManager(requesterId)
                ? absenceRepository.findAllByStatusOrderByCreatedAtAsc(AbsenceStatus.REQUESTED)
                : absenceRepository.findAllByActivityIdInAndStatusOrderByCreatedAtAsc(
                        authorizer.leadingActivityIds(requesterId), AbsenceStatus.REQUESTED);
        return list.stream().map(AbsenceResponse::from).toList();
    }

    @Transactional
    public AbsenceResponse approve(Long absenceId, Long reviewerId) {
        Absence absence = getReviewable(absenceId, reviewerId);
        LocalDateTime now = LocalDateTime.now();
        absence.approve(reviewerId, now);
        attendanceRepository.findBySessionIdAndMemberId(absence.getSessionId(), absence.getMemberId())
                .filter(a -> a.getStatus() != AttendanceStatus.PRESENT)
                .ifPresent(a -> a.changeStatus(AttendanceStatus.EXCUSED, now));
        return AbsenceResponse.from(absence);
    }

    @Transactional
    public AbsenceResponse reject(Long absenceId, Long reviewerId, AbsenceRejectRequest req) {
        Absence absence = getReviewable(absenceId, reviewerId);
        absence.reject(reviewerId, req.comment(), LocalDateTime.now());   // 출석 상태는 그대로 (참석 대상 유지)
        return AbsenceResponse.from(absence);
    }

    private Absence getReviewable(Long absenceId, Long reviewerId) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "불참 요청을 찾을 수 없습니다."));
        attendanceService.requireLeader(absence.getActivityId(), reviewerId);
        return absence;
    }
}

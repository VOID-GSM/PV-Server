package pv.domain.attendance.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.attendance.dto.request.AbsenceRejectRequest;
import pv.domain.attendance.dto.request.AbsenceRequest;
import pv.domain.attendance.dto.request.BulkAttendanceRequest;
import pv.domain.attendance.dto.request.CheckInRequest;
import pv.domain.attendance.dto.response.AbsenceResponse;
import pv.domain.attendance.dto.response.AttendanceResponse;
import pv.domain.attendance.dto.response.AttendanceStatsResponse;
import pv.domain.attendance.service.AbsenceService;
import pv.domain.attendance.service.AttendanceService;

import java.util.List;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final AbsenceService absenceService;

    @PostMapping("/sessions/{sessionId}/check-in")
    public AttendanceResponse checkIn(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long me,
            @RequestBody(required = false)CheckInRequest req) {
        return attendanceService.checkIn(sessionId, me, req == null ? new CheckInRequest(null) : req);
    }

    @PatchMapping("/sessions/{sessionId}")
    public List<AttendanceResponse> bulkUpdate(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long me,
            @RequestBody @Valid BulkAttendanceRequest req) {
        return attendanceService.bulkUpdate(sessionId, me, req);
    }

    @GetMapping("/sessions/{sessionId}")
    public List<AttendanceResponse> getSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Long me) {
        return attendanceService.getSessionAttendance(sessionId, me);
    }

    @PostMapping("/absences")
    @ResponseStatus(HttpStatus.CREATED)
    public AbsenceResponse requestAbsence(
            @AuthenticationPrincipal Long me,
            @RequestBody @Valid AbsenceRequest req) {
        return absenceService.request(me, req);
    }

    @GetMapping("/absences")
    public List<AbsenceResponse> getPendingAbsences(
            @AuthenticationPrincipal Long me) {
        return absenceService.getPending(me);
    }

    @PostMapping("/absences/{absenceId}/approve")
    public AbsenceResponse approve(
            @PathVariable Long absenceId,
            @AuthenticationPrincipal Long me) {
        return absenceService.approve(absenceId, me);
    }

    @PostMapping("/absences/{absenceId}/reject")
    public AbsenceResponse reject(
            @PathVariable Long absenceId,
            @AuthenticationPrincipal Long me,
            @RequestBody @Valid AbsenceRejectRequest req) {
        return absenceService.reject(absenceId, me, req);
    }

    @GetMapping("/stats/members/{memberId}")
    public List<AttendanceStatsResponse> memberStats(
            @PathVariable Long memberId,
            @AuthenticationPrincipal Long me) {
        return attendanceService.getMemberStats(memberId, me);
    }

    @GetMapping("/stats/activities/{activityId}")
    public List<AttendanceStatsResponse> activityStats(
            @PathVariable Long activityId,
            @AuthenticationPrincipal Long me) {
        return attendanceService.getActivityStats(activityId, me);
    }
}

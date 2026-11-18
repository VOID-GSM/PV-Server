package pv.domain.attendance.dto.response;

import pv.domain.attendance.entity.Attendance;
import pv.domain.attendance.entity.AttendanceStatus;

import java.time.LocalDateTime;

public record AttendanceResponse(
        Long memberId,
        AttendanceStatus status,
        LocalDateTime checkedAt
) {
    public static AttendanceResponse from(Attendance a) {
        return new AttendanceResponse(a.getMemberId(), a.getStatus(), a.getCheckedAt());
    }
}

package pv.domain.attendance.dto;

import pv.domain.attendance.entity.AttendanceStatus;

public record StatusCount(
        Long groupId,
        AttendanceStatus status,
        Long count
) {
}

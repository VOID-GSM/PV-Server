package pv.domain.attendance.dto.response;

import pv.domain.attendance.entity.Absence;
import pv.domain.attendance.entity.AbsenceStatus;

import java.time.LocalDateTime;

public record AbsenceResponse(
        Long id,
        Long sessionId,
        Long activityId,
        Long memberId,
        String reason,
        AbsenceStatus status,
        String rejectComment,
        LocalDateTime createdAt
) {
    public static AbsenceResponse from(Absence a) {
        return new AbsenceResponse(
                a.getId(),
                a.getSessionId(),
                a.getActivityId(),
                a.getMemberId(),
                a.getReason(),
                a.getStatus(),
                a.getRejectComment(),
                a.getCreatedAt());
    }
}

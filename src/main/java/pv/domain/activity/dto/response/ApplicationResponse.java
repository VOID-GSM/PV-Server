package pv.domain.activity.dto.response;

import pv.domain.activity.entity.ActivityApplication;
import pv.domain.activity.entity.ApplicationStatus;

import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id, Long memberId, String name,
        ApplicationStatus status, String statusLabel, LocalDateTime createdAt
) {
    public static ApplicationResponse of(ActivityApplication a, String name) {
        return new ApplicationResponse(a.getId(), a.getMemberId(), name,
                a.getStatus(), a.getStatus().getLabel(), a.getCreatedAt());
    }
}
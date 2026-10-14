package pv.domain.activity.dto.response;

import pv.domain.activity.entity.*;

import java.time.LocalDate;

public record ActivitySummaryResponse(
        Long id, String name,
        ActivityType type, String typeLabel,
        RecruitMode recruitMode, String recruitModeLabel,
        ActivityStatus status, String statusLabel,
        int capacity, long memberCount,
        LocalDate startDate, LocalDate endDate
) {
    public static ActivitySummaryResponse of(Activity a, long memberCount) {
        return new ActivitySummaryResponse(a.getId(), a.getName(),
                a.getType(), a.getType().getLabel(),
                a.getRecruitMode(), a.getRecruitMode().getLabel(),
                a.getStatus(), a.getStatus().getLabel(),
                a.getCapacity(), memberCount, a.getStartDate(), a.getEndDate());
    }
}
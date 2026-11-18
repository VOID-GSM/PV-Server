package pv.domain.reports.dto;

import pv.domain.reports.entity.Report;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        String content,
        LocalDateTime generatedAt
) {
    public static ReportResponse from(Report r) {
        return new ReportResponse(r.getWeekStart(), r.getWeekStart().plusDays(6),
                r.getContent(), r.getGeneratedAt());
    }
}

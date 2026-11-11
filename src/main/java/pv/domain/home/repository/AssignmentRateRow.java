package pv.domain.home.repository;

public record AssignmentRateRow(
        Long memberId,
        long dueCount,
        long submittedCount
) {
}

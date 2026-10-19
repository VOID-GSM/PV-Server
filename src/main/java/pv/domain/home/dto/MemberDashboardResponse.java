package pv.domain.home.dto;

public record MemberDashboardResponse(
        Long memberId,
        String name,
        Integer generation,
        long boardCount,
        long weeklyDeadlineCount,
        boolean overloaded,
        long presentCount,
        long excusedAbsenceCount,
        long unexcusedAbsenceCount,
        Double attendanceRate,
        long dueAssignmentCount,
        long submittedAssignmentCount,
        Double assignmentSubmissionRate
) {
}

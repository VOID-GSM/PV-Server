package pv.domain.home.repository;

public record DashboardMemberRow(
        Long memberId,
        String name,
        Integer generation
) {
}

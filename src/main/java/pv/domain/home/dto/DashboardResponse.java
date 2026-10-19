package pv.domain.home.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        int overloadedMemberCount,
        List<MemberDashboardResponse> members
) {
}

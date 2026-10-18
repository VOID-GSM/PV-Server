package pv.domain.calendar.port;

import java.util.Set;

public interface CalendarMembershipPort {
    Set<Long> boardIdsOf(Long memberId);
    Set<Long> activityIdsOf(Long memberId);
}

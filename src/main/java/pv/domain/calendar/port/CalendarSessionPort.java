package pv.domain.calendar.port;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface CalendarSessionPort {
    List<SessionView> findSessions(Collection<Long> activityIds, LocalDate from, LocalDate to);

    record SessionView(Long id, Long activityId, String activityName, int sessionNo,
                       LocalDate date, LocalTime time, String topic) {}
}

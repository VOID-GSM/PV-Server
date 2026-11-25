package pv.domain.calendar.port;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CalendarDeadlinePort {
    List<DeadlineView> findDeadlines(Collection<Long> boardIds, LocalDate from, LocalDate to);
    record DeadlineView(Long id, Long boardId, String name, LocalDate date, Set<Long> assigneeIds, String description) {}
}
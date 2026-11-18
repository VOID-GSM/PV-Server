package pv.domain.calendar.port;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pv.domain.board.entity.Deadline;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CalendarDeadlineAdapter implements CalendarDeadlinePort {
    private final EntityManager em;

    @Override
    public List<DeadlineView> findDeadlines(Collection<Long> boardIds, LocalDate from, LocalDate to) {
        if (boardIds == null || boardIds.isEmpty()) return List.of();

        return em.createQuery("""
                        select d from Deadline d
                        where d.boardId in :boardIds
                          and d.deadlineDate between :from and :to
                        order by d.deadlineDate, d.id
                        """, Deadline.class)
                .setParameter("boardIds", boardIds)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList()
                .stream()
                .map(d -> new DeadlineView(d.getId(), d.getBoardId(), d.getName(),
                        d.getDeadlineDate(), d.getAssigneeId()))
                .toList();
    }
}

package pv.domain.calendar.port;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pv.domain.activity.entity.Activity;
import pv.domain.activity.entity.ActivitySession;
import pv.domain.activity.entity.SessionStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CalendarSessionAdapter implements CalendarSessionPort {
    private final EntityManager em;

    @Override
    public List<SessionView> findSessions(Collection<Long> activityIds, LocalDate from, LocalDate to) {
        if (activityIds == null || activityIds.isEmpty()) return List.of();

        return em.createQuery("""
                    select new pv.domain.calendar.port.CalendarSessionPort$SessionView(
                        s.id, s.activityId, a.name, s.sessionNo, s.sessionDate, s.sessionTime, s.topic)
                    from ActivitySession s
                    join Activity a on a.id = s.activityId
                    where s.activityId in :activityIds
                      and s.sessionDate between :from and :to
                      and s.status <> :cancelled
                    """, SessionView.class)
                .setParameter("activityIds", activityIds)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("cancelled", SessionStatus.CANCELLED)
                .getResultList();
    }
}

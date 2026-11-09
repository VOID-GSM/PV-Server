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
                        select s, a from ActivitySession s, Activity a
                        where a.id = s.activityId
                          and s.activityId in :activityIds
                          and s.sessionDate between :from and :to
                          and s.status <> :cancelled
                        order by s.sessionDate, s.sessionTime, s.id
                        """, Object[].class)
                .setParameter("activityIds", activityIds)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("cancelled", SessionStatus.CANCELLED)
                .getResultList()
                .stream()
                .map(row -> {
                    ActivitySession s = (ActivitySession) row[0];
                    Activity a = (Activity) row[1];
                    return new SessionView(s.getId(), s.getActivityId(), a.getName(), s.getSessionNo(),
                            s.getSessionDate(), s.getSessionTime(), s.getTopic());
                })
                .toList();
    }
}

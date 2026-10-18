package pv.domain.calendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.calendar.entity.CalendarEvent;
import pv.domain.calendar.entity.EventType;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    @Query("""
            select e from CalendarEvent e
            where e.startsAt < :to
              and (e.endsAt > :from or e.startsAt >= :from)
              and (
                   (e.type = :personal and e.ownerId = :memberId)
                or (e.boardId is not null and (:allBoards = true or e.boardId in :boardIds))
                or (e.activityId is not null and (:allActivities = true or e.activityId in :activityIds))
              )
            order by e.startsAt
            """)
    List<CalendarEvent> findCalendar(@Param("memberId") Long memberId,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     @Param("allBoards") boolean allBoards,
                                     @Param("boardIds") Collection<Long> boardIds,
                                     @Param("allActivities") boolean allActivities,
                                     @Param("activityIds") Collection<Long> activityIds,
                                     @Param("personal") EventType personal);

    @Query("""
            select e from CalendarEvent e
            where e.type = :deadline and e.boardId in :boardIds
              and e.startsAt >= :from and e.startsAt < :to
            order by e.startsAt
            """)
    List<CalendarEvent> findDeadlines(@Param("boardIds") Collection<Long> boardIds,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to,
                                      @Param("deadline") EventType deadline);

    @Query("""
        select e from CalendarEvent e
        where e.startsAt < :to
          and (e.endsAt > :from or e.startsAt >= :from)
          and (
               (e.type = :personal and e.ownerId = :memberId)
            or e.boardId in :boardIds
            or e.activityId in :activityIds
          )
        order by e.startsAt
        """)
    List<CalendarEvent> findCalendar(@Param("memberId") Long memberId,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     @Param("boardIds") Collection<Long> boardIds,
                                     @Param("activityIds") Collection<Long> activityIds,
                                     @Param("personal") EventType personal);
}
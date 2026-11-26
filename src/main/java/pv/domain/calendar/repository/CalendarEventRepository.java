package pv.domain.calendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    default void deleteAllByBoardId(Long boardId) {
        deleteAssigneesByBoardId(boardId);
        deleteEventsByBoardId(boardId);
    }

    @Modifying(flushAutomatically = true)
    @Query(value = """
            delete from calendar_event_assignees
            where event_id in (select id from calendar_event where board_id = :boardId)
            """, nativeQuery = true)
    void deleteAssigneesByBoardId(@Param("boardId") Long boardId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CalendarEvent e where e.boardId = :boardId")
    void deleteEventsByBoardId(@Param("boardId") Long boardId);
}

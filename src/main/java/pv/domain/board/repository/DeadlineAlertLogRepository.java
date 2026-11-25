package pv.domain.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.board.entity.DeadlineAlertLog;

import java.util.Collection;
import java.util.List;

public interface DeadlineAlertLogRepository extends JpaRepository<DeadlineAlertLog, Long> {

    List<DeadlineAlertLog> findAllByDeadlineIdIn(Collection<Long> deadlineIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from DeadlineAlertLog l where l.deadlineId in :deadlineIds")
    void deleteAllByDeadlineIds(@Param("deadlineIds") Collection<Long> deadlineIds);
}

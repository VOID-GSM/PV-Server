package pv.domain.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.attendance.entity.AttendanceSession;

import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    List<AttendanceSession> findAllByClosedFalseAndCheckInClosesAtBefore(LocalDateTime now);

    List<AttendanceSession> findAllByActivityIdAndCheckInClosesAtAfter(Long activityId, LocalDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("delete from AttendanceSession s where s.activityId = :activityId")
    void deleteAllByActivityId(@Param("activityId") Long activityId);
}
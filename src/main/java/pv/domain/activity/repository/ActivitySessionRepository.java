package pv.domain.activity.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.activity.entity.ActivitySession;
import pv.domain.activity.entity.SessionStatus;
import pv.domain.activity.entity.TopicMode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivitySessionRepository extends JpaRepository<ActivitySession, Long> {

    List<ActivitySession> findAllByActivityIdOrderBySessionNoAsc(Long activityId);

    Optional<ActivitySession> findByIdAndActivityId(Long id, Long activityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ActivitySession> findWithLockById(Long id);

    List<ActivitySession> findTop3ByActivityIdAndStatusAndSessionDateGreaterThanEqualOrderBySessionDateAsc(
            Long activityId, SessionStatus status, LocalDate date);

    // 회차 날짜가 오늘이거나 과거이며, 주제가 아직 확정되지 않은 투표형 회차
    @Query("select s from ActivitySession s where s.topic is null and s.status = :status "
            + "and s.sessionDate <= :today "
            + "and s.activityId in (select a.id from Activity a where a.topicMode = :mode)")
    List<ActivitySession> findClosedVoteSessions(
            @Param("today") LocalDate today,
            @Param("status") SessionStatus status,
            @Param("mode") TopicMode mode
    );
    @Modifying(flushAutomatically = true)
    @Query("delete from ActivitySession s where s.activityId = :activityId")
    void deleteAllByActivityId(@Param("activityId") Long activityId);
}
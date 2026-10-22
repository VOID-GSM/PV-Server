package pv.domain.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.attendance.entity.Absence;
import pv.domain.attendance.entity.AbsenceStatus;

import java.util.Collection;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    boolean existsBySessionIdAndMemberIdAndStatusIn(Long sessionId, Long memberId, Collection<AbsenceStatus> statuses);

    List<Absence> findAllByActivityIdInAndStatusOrderByCreatedAtAsc(Collection<Long> activityIds, AbsenceStatus status);

    List<Absence> findAllByStatusOrderByCreatedAtAsc(AbsenceStatus status);

    @Modifying(flushAutomatically = true)
    @Query("delete from Absence a where a.memberId = :memberId and a.sessionId in :sessionIds")
    void deleteByMemberAndSessions(@Param("memberId") Long memberId,
                                   @Param("sessionIds") Collection<Long> sessionIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from Absence a where a.activityId = :activityId")
    void deleteAllByActivityId(@Param("activityId") Long activityId);
}

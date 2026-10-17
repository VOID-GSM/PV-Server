package pv.domain.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.attendance.dto.StatusCount;
import pv.domain.attendance.entity.Attendance;
import pv.domain.attendance.entity.AttendanceStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findBySessionIdAndMemberId(Long sessionId, Long memberId);

    List<Attendance> findAllBySessionId(Long sessionId);

    List<Attendance> findAllBySessionIdAndMemberIdIn(Long sessionId, Collection<Long> memberIds);

    @Query("""
            select new pv.domain.attendance.dto.StatusCount(a.activityId, a.status, count(a))
            from Attendance a
            where a.memberId = :memberId
              and a.status <> pv.domain.attendance.entity.AttendanceStatus.PENDING
            group by a.activityId, a.status
            """)
    List<StatusCount> countByMemberGroupByActivity(@Param("memberId") Long memberId);

    @Query("""
            select new pv.domain.attendance.dto.StatusCount(a.memberId, a.status, count(a))
            from Attendance a
            where a.activityId = :activityId
              and a.status <> pv.domain.attendance.entity.AttendanceStatus.PENDING
            group by a.memberId, a.status
            """)
    List<StatusCount> countByActivityGroupByMember(@Param("activityId") Long activityId);

    /** 체크인 마감 후 미체크 → 무단 불참 (스케줄러에서 @Transactional 서비스를 통해 호출) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Attendance a set a.status = :to where a.sessionId = :sessionId and a.status = :from")
    int bulkChangeStatus(@Param("sessionId") Long sessionId,
                         @Param("from") AttendanceStatus from,
                         @Param("to") AttendanceStatus to);
}

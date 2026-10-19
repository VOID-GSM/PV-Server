package pv.domain.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.attendance.entity.Absence;
import pv.domain.attendance.entity.AbsenceStatus;

import java.util.Collection;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    boolean existsBySessionIdAndMemberIdAndStatusIn(Long sessionId, Long memberId, Collection<AbsenceStatus>statuses);
    List<Absence> findAllByActivityIdInAndStatusOrderByCreatedAtAsc(Collection<Long> activityIds, AbsenceStatus status);
    List<Absence> findAllByStatusOrderByCreatedAtAsc(AbsenceStatus status);
}

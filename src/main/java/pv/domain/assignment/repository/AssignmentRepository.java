package pv.domain.assignment.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import pv.domain.assignment.entity.Assignment;
import pv.domain.member.entity.Major;

import java.util.Collection;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    @Query("select a from Assignment a join fetch a.author where a.id = :id")
    Optional<Assignment> findDetail(Long id);

    // 제출: 공유 락 → 마감 시간 수정/삭제(쓰기 락)와 직렬화
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select a from Assignment a join fetch a.author where a.id = :id")
    Optional<Assignment> findByIdForShare(Long id);

    // 수정/삭제: 배타 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Assignment a where a.id = :id")
    Optional<Assignment> findByIdForUpdate(Long id);

    @Query(value = """
            select a from Assignment a join fetch a.author
            where a.teamId = :teamId
              and a.author.id <> :memberId
              and (a.targetType = pv.domain.assignment.entity.AssignmentTargetType.ALL
                   or (a.targetType = pv.domain.assignment.entity.AssignmentTargetType.MEMBER
                       and exists (select 1 from a.targetMemberIds t where t = :memberId))
                   or (a.targetType = pv.domain.assignment.entity.AssignmentTargetType.MAJOR
                       and exists (select 1 from a.targetMajors tm where tm in :majors)))
            order by a.id desc
            """,
            countQuery = """
                    select count(a) from Assignment a
                    where a.teamId = :teamId
                      and a.author.id <> :memberId
                      and (a.targetType = pv.domain.assignment.entity.AssignmentTargetType.ALL
                           or (a.targetType = pv.domain.assignment.entity.AssignmentTargetType.MEMBER
                               and exists (select 1 from a.targetMemberIds t where t = :memberId))
                           or (a.targetType = pv.domain.assignment.entity.AssignmentTargetType.MAJOR
                               and exists (select 1 from a.targetMajors tm where tm in :majors)))
                    """)
    Page<Assignment> findReceived(Long teamId, Long memberId, Collection<Major> majors, Pageable pageable);

    @Query(value = """
            select a from Assignment a join fetch a.author
            where a.author.id = :memberId
            order by a.id desc
            """,
            countQuery = "select count(a) from Assignment a where a.author.id = :memberId")
    Page<Assignment> findCreated(Long memberId, Pageable pageable);
}

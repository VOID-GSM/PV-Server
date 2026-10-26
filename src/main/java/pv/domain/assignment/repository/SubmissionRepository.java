package pv.domain.assignment.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pv.domain.assignment.dto.projection.MySubmissionRow;
import pv.domain.assignment.dto.projection.SubmissionCountRow;
import pv.domain.assignment.dto.projection.SubmissionRow;
import pv.domain.assignment.entity.Submission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByAssignmentIdAndSubmitterId(Long assignmentId, Long submitterId);
    List<Submission> findAllByAssignmentId(Long assignmentId);

    // 상세: 제출자/과제/출제자까지 한 번에
    @Query("""
            select s from Submission s
            join fetch s.submitter
            join fetch s.assignment a
            join fetch a.author
            where s.id = :id
            """)
    Optional<Submission> findDetail(Long id);

    // 통과/재제출 요청/재제출: 동일 제출물 상태 변경 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Submission s where s.id = :id")
    Optional<Submission> findByIdForUpdate(Long id);

    // late는 저장값이 아니라 현재 마감과 비교해 계산
    @Query("""
            select new pv.domain.assignment.dto.projection.SubmissionRow(
                s.id, s.submitter.id, s.status,
                case when s.submittedAt > a.deadline then true else false end,
                s.submittedAt)
            from Submission s join s.assignment a
            where a.id = :assignmentId
            """)
    List<SubmissionRow> findRows(Long assignmentId);

    @Query("""
            select new pv.domain.assignment.dto.projection.MySubmissionRow(
                a.id, s.status,
                case when s.submittedAt > a.deadline then true else false end)
            from Submission s join s.assignment a
            where s.submitter.id = :memberId and a.id in :assignmentIds
            """)
    List<MySubmissionRow> findMine(Long memberId, Collection<Long> assignmentIds);

    @Query("""
            select new pv.domain.assignment.dto.projection.SubmissionCountRow(s.assignment.id, count(s))
            from Submission s where s.assignment.id in :assignmentIds
            group by s.assignment.id
            """)
    List<SubmissionCountRow> countByAssignmentIds(Collection<Long> assignmentIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from Submission s where s.assignment.id = :assignmentId")
    void deleteAllByAssignmentId(Long assignmentId);
}
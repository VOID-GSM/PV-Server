package pv.domain.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.assignment.entity.AssignmentRecipient;

import java.util.Collection;

public interface AssignmentRecipientRepository extends JpaRepository<AssignmentRecipient, Long> {

    boolean existsByAssignmentIdAndMemberId(Long assignmentId, Long memberId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into assignment_recipients (assignment_id, member_id)
            select :assignmentId, m.id from member m
            where m.id in (:memberIds)
            on conflict do nothing
            """, nativeQuery = true)
    int insertAll(@Param("assignmentId") Long assignmentId,
                  @Param("memberIds") Collection<Long> memberIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from AssignmentRecipient r where r.assignmentId = :assignmentId")
    void deleteAllByAssignmentId(@Param("assignmentId") Long assignmentId);
}
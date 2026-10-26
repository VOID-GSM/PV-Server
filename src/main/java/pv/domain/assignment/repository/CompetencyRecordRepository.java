package pv.domain.assignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pv.domain.assignment.entity.CompetencyRecord;

public interface CompetencyRecordRepository extends JpaRepository<CompetencyRecord, Long> {

    @Modifying(flushAutomatically = true)
    @Query("delete from CompetencyRecord c where c.assignmentId = :assignmentId")
    void deleteAllByAssignmentId(Long assignmentId);
}

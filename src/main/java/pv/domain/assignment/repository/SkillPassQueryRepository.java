package pv.domain.assignment.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import pv.domain.assignment.dto.projection.SkillPassCountRow;
import pv.domain.assignment.entity.Submission;

import java.util.Collection;
import java.util.List;

// 역량 맵용: 멤버가 통과한 제출물 수를 스킬별로 집계 (DTO 직접 조회, 1회 쿼리)
public interface SkillPassQueryRepository extends Repository<Submission, Long> {

    @Query("""
            select new pv.domain.assignment.dto.projection.SkillPassCountRow(sk, count(s))
            from Submission s
            join s.assignment a
            join a.skillIds sk
            where s.submitter.id = :memberId
              and s.status = pv.domain.assignment.entity.SubmissionStatus.PASSED
              and sk in :skillIds
            group by sk
            """)
    List<SkillPassCountRow> countPassedBySkill(Long memberId, Collection<Long> skillIds);
}
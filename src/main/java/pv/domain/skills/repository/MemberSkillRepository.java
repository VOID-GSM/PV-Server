package pv.domain.skills.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.skills.entity.MemberSkill;

import java.util.Collection;
import java.util.List;

public interface MemberSkillRepository extends JpaRepository<MemberSkill, Long> {
    @Query("""
            select ms from MemberSkill ms
            where ms.member.id = :memberId
              and ms.skill.id in :skillIds
            """)
    List<MemberSkill> findByMemberIdAndSkillIds(@Param("memberId") Long memberId,
                                                @Param("skillIds") Collection<Long> skillIds);
}

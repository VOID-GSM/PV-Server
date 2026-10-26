package pv.domain.skills.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.member.entity.Major;
import pv.domain.skills.entity.Skill;

import java.util.Collection;
import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findAllByActiveTrueOrderByMajorAscSortOrderAscIdAsc();
    List<Skill> findAllByMajorAndActiveTrueOrderBySortOrderAscIdAsc(Major major);
    List<Skill> findAllByMajorInAndActiveTrueOrderByMajorAscSortOrderAscIdAsc(Collection<Major> majors);
    boolean existsByMajorAndName(Major major, String name);
    boolean existsByMajorAndNameAndIdNot(Major major, String name, Long id);

    // 과제 출제·수정 검증용: 요청 ID 중 활성 스킬만 1회 조회 (결과 수 != 요청 수면 없거나 비활성)
    List<Skill> findAllByIdInAndActiveTrue(Collection<Long> ids);
}
package pv.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.member.entity.Generation;

import java.util.List;
import java.util.Optional;

public interface GenerationRepository extends JpaRepository<Generation, Long> {

    List<Generation> findAllByTeamIdOrderByNumberAsc(Long teamId);

    // 현재 활동 기수 = 팀의 가장 최신(가장 큰 번호) 기수. 기수가 없으면 비어 있다.
    @Query("select max(g.number) from Generation g where g.teamId = :teamId")
    Optional<Integer> findCurrentNumberByTeamId(@Param("teamId") Long teamId);
}
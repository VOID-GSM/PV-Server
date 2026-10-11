package pv.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.member.entity.Generation;

import java.util.List;

public interface GenerationRepository extends JpaRepository<Generation, Long> {

    List<Generation> findAllByTeamIdOrderByNumberAsc(Long teamId);
}
package pv.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.auth.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {}
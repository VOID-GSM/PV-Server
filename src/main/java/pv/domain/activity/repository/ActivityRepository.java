package pv.domain.activity.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.activity.entity.Activity;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findAllByTeamIdOrderByIdDesc(Long teamId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Activity> findWithLockById(Long id);

    @Query("select a.id from Activity a where a.teamId = :teamId")
    List<Long> findIdsByTeamId(@Param("teamId") Long teamId);
}
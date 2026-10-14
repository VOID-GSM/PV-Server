package pv.domain.activity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.activity.entity.ActivityApplication;

import java.util.List;
import java.util.Optional;

public interface ActivityApplicationRepository extends JpaRepository<ActivityApplication, Long> {

    List<ActivityApplication> findAllByActivityIdOrderByCreatedAtAsc(Long activityId);

    Optional<ActivityApplication> findByIdAndActivityId(Long id, Long activityId);

    Optional<ActivityApplication> findByActivityIdAndMemberId(Long activityId, Long memberId);

    boolean existsByActivityIdAndMemberId(Long activityId, Long memberId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ActivityApplication a where a.activityId = :activityId")
    void deleteAllByActivityId(@Param("activityId") Long activityId);
}
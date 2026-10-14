package pv.domain.activity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.activity.dto.query.MemberCount;
import pv.domain.activity.entity.ActivityMember;
import pv.domain.activity.entity.ActivityRole;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActivityMemberRepository extends JpaRepository<ActivityMember, Long> {

    List<ActivityMember> findAllByActivityIdOrderByJoinedAtAsc(Long activityId);

    Optional<ActivityMember> findByActivityIdAndMemberId(Long activityId, Long memberId);

    boolean existsByActivityIdAndMemberId(Long activityId, Long memberId);

    long countByActivityId(Long activityId);

    long countByActivityIdAndRole(Long activityId, ActivityRole role);

    @Query("select new pv.domain.activity.dto.query.MemberCount(m.activityId, count(m)) "
            + "from ActivityMember m where m.activityId in :activityIds group by m.activityId")
    List<MemberCount> countByActivityIds(@Param("activityIds") Collection<Long> activityIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from ActivityMember m where m.activityId = :activityId")
    void deleteAllByActivityId(@Param("activityId") Long activityId);
}
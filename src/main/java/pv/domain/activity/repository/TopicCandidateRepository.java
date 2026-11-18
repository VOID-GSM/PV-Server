package pv.domain.activity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.activity.entity.TopicCandidate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TopicCandidateRepository extends JpaRepository<TopicCandidate, Long> {

    List<TopicCandidate> findAllBySessionIdOrderByIdAsc(Long sessionId);

    List<TopicCandidate> findAllBySessionIdIn(Collection<Long> sessionIds);

    Optional<TopicCandidate> findByIdAndSessionId(Long id, Long sessionId);

    boolean existsBySessionIdAndTitle(Long sessionId, String title);

    @Modifying(flushAutomatically = true)
    @Query("delete from TopicCandidate c where c.sessionId in "
            + "(select s.id from ActivitySession s where s.activityId = :activityId)")
    void deleteAllByActivityId(@Param("activityId") Long activityId);
}
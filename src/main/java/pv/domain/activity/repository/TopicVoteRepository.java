package pv.domain.activity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.activity.dto.query.VoteCount;
import pv.domain.activity.entity.TopicVote;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TopicVoteRepository extends JpaRepository<TopicVote, Long> {

    Optional<TopicVote> findBySessionIdAndMemberId(Long sessionId, Long memberId);

    Optional<TopicVote> findBySessionIdAndMemberIdAndCandidateId(Long sessionId, Long memberId, Long candidateId);

    @Query("select new pv.domain.activity.dto.query.VoteCount(v.sessionId, v.candidateId, count(v)) "
            + "from TopicVote v where v.sessionId in :sessionIds group by v.sessionId, v.candidateId")
    List<VoteCount> countBySessionIds(@Param("sessionIds") Collection<Long> sessionIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from TopicVote v where v.sessionId in "
            + "(select s.id from ActivitySession s where s.activityId = :activityId)")
    void deleteAllByActivityId(@Param("activityId") Long activityId);

    @Modifying(flushAutomatically = true)
    @Query("delete from TopicVote v where v.memberId = :memberId and v.sessionId in "
            + "(select s.id from ActivitySession s where s.activityId = :activityId)")
    void deleteAllByActivityIdAndMemberId(@Param("activityId") Long activityId, @Param("memberId") Long memberId);
}
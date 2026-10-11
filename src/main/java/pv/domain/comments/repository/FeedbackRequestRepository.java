package pv.domain.comments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.comments.entity.FeedbackRequest;
import pv.domain.comments.entity.TargetType;

import java.util.List;

public interface FeedbackRequestRepository extends JpaRepository<FeedbackRequest, Long> {
    List<FeedbackRequest> findByReceiverIdAndCompletedAtIsNullOrderByCreatedAtDesc(Long receiverId);
    @Query("""
        select r.receiverId from FeedbackRequest r
        where r.targetType = :type and r.targetId = :targetId and r.completedAt is null
        """)
    List<Long> findPendingReceiverIds(
            @Param("type") TargetType type,
            @Param("targetId") Long targetId);
}

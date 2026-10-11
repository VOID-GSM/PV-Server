package pv.domain.comments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.comments.entity.Comment;
import pv.domain.comments.entity.TargetType;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(TargetType targetType, Long targetId);
}

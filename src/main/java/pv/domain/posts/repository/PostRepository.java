package pv.domain.posts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pv.domain.posts.entity.Post;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    List<Post> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);
    List<Post> findAllByQuestionIdOrderByCreatedAtAsc(Long questionId);
}

package pv.domain.posts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.posts.entity.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    List<Post> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);
    List<Post> findAllByQuestionIdOrderByCreatedAtAsc(Long questionId);


    @Query("select p.authorId from Post p where p.id = :id")
    Optional<Long> findAuthorIdById(@Param("id") Long id);
}

package pv.domain.posts.repository;

import org.springframework.data.jpa.domain.Specification;
import pv.domain.posts.entity.Post;

import java.time.LocalDate;

public class PostSpecs {
    private PostSpecs() {}
    public static Specification<Post> activityId(Long activityId) {
        return (root, q, cb) ->
                activityId == null ? null : cb.equal(root.get("activityId"), activityId);
    }

    public static Specification<Post> sessionId(Long sessionId) {
        return (root, q, cb) ->
                sessionId == null ? null : cb.equal(root.get("sessionId"), sessionId);
    }

    public static Specification<Post> authorId(Long authorId) {
        return (root, q, cb) ->
                authorId == null ? null : cb.equal(root.get("authorId"), authorId);
    }

    public static Specification<Post> createdFrom(LocalDate from) {
        return (root, q, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
    }

    public static Specification<Post> createdTo(LocalDate to) {
        return (root, q, cb) ->
                to == null ? null : cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
    }
}

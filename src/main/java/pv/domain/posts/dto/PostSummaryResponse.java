package pv.domain.posts.dto;

import pv.domain.posts.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryResponse(
        Long id,
        Long authorId,
        String authorName,
        Long activityId,
        Long sessionId,
        Long questionId,
        String title,
        LocalDateTime createdAt,
        List<String> tags
) {
    public static PostSummaryResponse from(Post post, String authorName) {
        return new PostSummaryResponse(
                post.getId(),
                post.getAuthorId(),
                authorName,
                post.getActivityId(),
                post.getSessionId(),
                post.getQuestionId(),
                post.getTitle(),
                post.getCreatedAt(),
                post.getTags().stream().sorted().toList()
        );
    }
}

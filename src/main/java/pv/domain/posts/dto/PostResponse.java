package pv.domain.posts.dto;

import pv.domain.posts.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        Long authorId,
        String authorName,
        Long activityId,
        Long sessionId,
        Long questionId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> tags
) {
    public static PostResponse from(Post post, String authorName) {
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                authorName,
                post.getActivityId(),
                post.getSessionId(),
                post.getQuestionId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getTags().stream().sorted().toList()
        );
    }
}

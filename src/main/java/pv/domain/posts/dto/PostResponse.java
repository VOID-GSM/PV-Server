package pv.domain.posts.dto;

import pv.domain.posts.entity.Post;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        Long authorId,
        Long activityId,
        Long sessionId,
        Long questionId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getActivityId(),
                post.getSessionId(),
                post.getQuestionId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}

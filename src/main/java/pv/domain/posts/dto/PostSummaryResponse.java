package pv.domain.posts.dto;

import pv.domain.posts.entity.Post;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        Long authorId,
        Long activityId,
        Long sessionId,
        Long questionId,
        String title,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getAuthorId(),
                post.getActivityId(),
                post.getSessionId(),
                post.getQuestionId(),
                post.getTitle(),
                post.getCreatedAt()
        );
    }
}

package pv.domain.comments.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pv.domain.comments.entity.Anchor;
import pv.domain.comments.entity.Comment;
import pv.domain.comments.entity.TargetType;

import java.time.LocalDateTime;

public class CommentDto {
    public record AnchorRequest(
            @NotNull Integer blockIndex,
            @NotNull Integer startOffset,
            @NotNull Integer endOffset
    ) {}

    public record Create(
            @NotNull TargetType targetType,
            @NotNull Long targetId,
            @NotBlank @Size(max = 5000) String content,
            @Valid AnchorRequest anchor
    ) {}

    public record Update(@NotBlank @Size(max = 5000) String content) {}

    public record Response(
            Long id, Long authorId, String content, Anchor anchor,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        public static Response from(Comment c) {
            return new Response(c.getId(), c.getAuthorId(), c.getContent(), c.getAnchor(),
                    c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}

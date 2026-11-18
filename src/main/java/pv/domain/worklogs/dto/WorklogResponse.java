package pv.domain.worklogs.dto;

import pv.domain.worklogs.entity.Visibility;
import pv.domain.worklogs.entity.Worklog;

import java.time.LocalDateTime;

public record WorklogResponse(
        Long id,
        Long authorId,
        String authorName,
        Long boardId,
        String title,
        String content,
        Visibility visibility,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WorklogResponse from(Worklog w) {
        return new WorklogResponse(
                w.getId(),
                w.getAuthor().getId(),
                w.getAuthor().getName(),
                w.getBoard() == null ? null : w.getBoard().getId(),
                w.getTitle(),
                w.getContent(),
                w.getVisibility(),
                w.getCreatedAt(),
                w.getUpdatedAt()
        );
    }
}

package pv.domain.home.dto;

import pv.domain.home.type.TodoType;

import java.time.OffsetDateTime;

public record TodoItemResponse(
        TodoType type,
        String typeLabel,
        Long targetId,
        String title,
        String description,
        OffsetDateTime dueAt,
        boolean overdue
) {
    public static TodoItemResponse of(TodoType type, Long targetId, String title,
                                      String description, OffsetDateTime dueAt, OffsetDateTime now) {
        boolean overdue = dueAt != null && dueAt.isBefore(now);
        return new TodoItemResponse(type, type.getLabel(), targetId, title, description, dueAt, overdue);
    }
}

package pv.domain.home.repository;

public record FeedbackTodoRow(
        Long requestId,
        String targetType,
        Long targetId,
        String requesterName,
        String message
) {
}

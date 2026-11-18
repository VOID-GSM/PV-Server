package pv.domain.home.repository;

import java.time.LocalDateTime;

public record AssignmentTodoRow(
        Long assignmentId,
        String title,
        LocalDateTime deadline,
        String submissionStatus
) {
}

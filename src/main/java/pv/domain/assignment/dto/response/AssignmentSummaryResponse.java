package pv.domain.assignment.dto.response;

import pv.domain.assignment.dto.projection.MySubmissionRow;
import pv.domain.assignment.entity.Assignment;
import pv.domain.assignment.entity.AssignmentTargetType;
import pv.domain.assignment.entity.SubmissionStatus;

import java.time.LocalDateTime;

// RECEIVED: mySubmissionStatus/myLate 채움 (null이면 미제출), CREATED: submissionCount 채움
public record AssignmentSummaryResponse(
        Long id,
        String title,
        LocalDateTime deadline,
        AssignmentTargetType targetType,
        Long authorId,
        String authorName,
        SubmissionStatus mySubmissionStatus,
        Boolean myLate,
        Long submissionCount
) {
    public static AssignmentSummaryResponse of(Assignment a, MySubmissionRow mine, Long count) {
        return new AssignmentSummaryResponse(
                a.getId(), a.getTitle(), a.getDeadline(), a.getTargetType(),
                a.getAuthor().getId(), a.getAuthor().getName(),
                mine == null ? null : mine.status(),
                mine == null ? null : mine.late(),
                count);
    }
}

package pv.domain.assignment.dto.projection;

import pv.domain.assignment.entity.SubmissionStatus;

public record MySubmissionRow(Long assignmentId, SubmissionStatus status, boolean late) {
}

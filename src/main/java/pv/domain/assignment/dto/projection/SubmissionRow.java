package pv.domain.assignment.dto.projection;

import pv.domain.assignment.entity.SubmissionStatus;

import java.time.LocalDateTime;

public record SubmissionRow(Long id, Long submitterId, SubmissionStatus status, boolean late,
                            LocalDateTime submittedAt) {
}

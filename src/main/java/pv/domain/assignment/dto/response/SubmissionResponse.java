package pv.domain.assignment.dto.response;

import pv.domain.assignment.entity.Submission;
import pv.domain.assignment.entity.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionResponse(
        Long id,
        Long assignmentId,
        Long submitterId,
        String submitterName,
        SubmissionStatus status,
        boolean late,
        String content,
        List<String> fileKeys,
        String githubUrl,
        String feedback,
        LocalDateTime submittedAt,
        LocalDateTime lastSubmittedAt,
        LocalDateTime reviewedAt
) {
    public static SubmissionResponse from(Submission s) {
        return new SubmissionResponse(
                s.getId(), s.getAssignment().getId(), s.getSubmitter().getId(), s.getSubmitter().getName(),
                s.getStatus(), s.isLate(), s.getContent(), List.copyOf(s.getFileKeys()), s.getGithubUrl(),
                s.getFeedback(), s.getSubmittedAt(), s.getLastSubmittedAt(), s.getReviewedAt());
    }
}

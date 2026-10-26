package pv.domain.assignment.dto.response;

import pv.domain.assignment.entity.Assignment;
import pv.domain.assignment.entity.AssignmentTargetType;
import pv.domain.member.entity.Major;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record AssignmentDetailResponse(
        Long id,
        String title,
        String description,
        LocalDateTime deadline,
        AssignmentTargetType targetType,
        Set<Major> targetMajors,
        Set<Long> targetMemberIds,
        List<String> fileKeys,
        List<String> competencies,
        Long authorId,
        String authorName,
        LocalDateTime createdAt
) {
    public static AssignmentDetailResponse from(Assignment a) {
        return new AssignmentDetailResponse(
                a.getId(), a.getTitle(), a.getDescription(), a.getDeadline(), a.getTargetType(),
                Set.copyOf(a.getTargetMajors()), Set.copyOf(a.getTargetMemberIds()),
                List.copyOf(a.getFileKeys()), List.copyOf(a.getCompetencies()),
                a.getAuthor().getId(), a.getAuthor().getName(), a.getCreatedAt());
    }
}

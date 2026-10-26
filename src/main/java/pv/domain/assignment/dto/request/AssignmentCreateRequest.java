package pv.domain.assignment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pv.domain.assignment.entity.AssignmentTargetType;
import pv.domain.member.entity.Major;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record AssignmentCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotNull LocalDateTime deadline,
        @NotNull AssignmentTargetType targetType,
        Set<Major> targetMajors,
        Set<Long> targetMemberIds,
        @Size(max = 10) List<String> fileKeys,
        @Size(max = 20) List<@NotBlank @Size(max = 100) String> competencies
) {
}

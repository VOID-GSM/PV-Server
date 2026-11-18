package pv.domain.skills.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pv.domain.member.entity.Major;

public record SkillCreateRequest(
        @NotNull Major major,
        @NotBlank @Size(max = 50) String name,
        @Size(max = 255) String description,
        @Min(0) Integer sortOrder
) {
}
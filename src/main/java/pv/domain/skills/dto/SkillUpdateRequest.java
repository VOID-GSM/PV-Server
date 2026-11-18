package pv.domain.skills.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SkillUpdateRequest(
        @Size(max = 50)
        @Pattern(regexp = ".*\\S.*", message = "공백만 입력할 수 없습니다.")
        String name,
        @Size(max = 255) String description,
        @Min(0) Integer sortOrder,
        Boolean active
) {
}
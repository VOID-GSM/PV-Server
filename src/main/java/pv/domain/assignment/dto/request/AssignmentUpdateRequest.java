package pv.domain.assignment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

// null이면 변경하지 않음. 대상(target)은 수정 불가.
public record AssignmentUpdateRequest(
        @Size(min = 1, max = 200) String title,
        @Size(min = 1, max = 5000) String description,
        LocalDateTime deadline,
        @Size(max = 10) List<String> fileKeys,
        @Size(max = 20) List<@NotBlank @Size(max = 100) String> competencies
) {
}

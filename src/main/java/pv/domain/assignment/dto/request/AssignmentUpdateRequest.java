package pv.domain.assignment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

// null이면 변경하지 않음. 대상(target)은 수정 불가.
// skillIds: null = 변경 없음, 빈 집합 = 전부 제거
public record AssignmentUpdateRequest(
        @Size(min = 1, max = 200) String title,
        @Size(min = 1, max = 5000) String description,
        LocalDateTime deadline,
        @Size(max = 10) List<String> fileKeys,
        @Size(max = 20) Set<@NotNull @Positive Long> skillIds
) {
}
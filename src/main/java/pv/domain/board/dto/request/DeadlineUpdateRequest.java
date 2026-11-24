package pv.domain.board.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record DeadlineUpdateRequest(
        @Size(max = 100) @Pattern(regexp = "(?s).*\\S.*", message = "공백만 입력할 수 없습니다.") String name,
        LocalDate date,
        @Size(max = 20) Set<@NotNull Long> assigneeIds,
        @Size(max = 2000) String description
) {
}

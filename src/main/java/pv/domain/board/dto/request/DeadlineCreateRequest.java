package pv.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record DeadlineCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull LocalDate date,
        @Size(max = 20) Set<@NotNull Long> assigneeIds,
        @Size(max = 2000) String description
) {
}

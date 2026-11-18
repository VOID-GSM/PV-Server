package pv.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DeadlineCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull LocalDate date,
        Long assigneeId
) {
}

package pv.domain.board.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pv.domain.board.entity.BoardType;

import java.time.LocalDate;
import java.util.Set;

public record BoardCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull BoardType type,
        @NotBlank @Size(max = 20) String color,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 10) Set<@Min(0) @Max(365) Integer> alertDays
) {
}

package pv.domain.board.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MilestoneSuggestRequest(
        @NotNull LocalDate finalDeadline
) {
}

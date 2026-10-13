package pv.domain.board.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record MilestoneConfirmRequest(
        @NotEmpty @Size(max = 50) List<@Valid Item> milestones
) {
    public record Item(
            @NotBlank @Size(max = 100) String name,
            @NotNull LocalDate date,
            Long assigneeId
    ) {
    }
}

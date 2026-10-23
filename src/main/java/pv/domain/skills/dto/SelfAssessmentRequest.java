package pv.domain.skills.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SelfAssessmentRequest(
        @NotEmpty @Size(max = 100)
        List<@Valid @NotNull ScoreItem> scores
) {
    public record ScoreItem(
            @NotNull Long skillId,
            @NotNull @Min(1) @Max(5) Integer score
    ) {

    }
}
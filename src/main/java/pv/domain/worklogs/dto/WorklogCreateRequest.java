package pv.domain.worklogs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pv.domain.worklogs.entity.Visibility;

public record WorklogCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 10000) String content,
        @NotNull Visibility visibility,
        Long boardId
) {
}

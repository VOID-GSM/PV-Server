package pv.domain.worklogs.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pv.domain.worklogs.entity.Visibility;

public record WorklogUpdateRequest(
        @Size(max = 100)
        @Pattern(regexp = ".*\\S.*", message = "제목은 공백만 입력할 수 없습니다.")
        String title,

        @Size(max = 10000)
        @Pattern(regexp = "(?s).*\\S.*", message = "내용은 공백만 입력할 수 없습니다.")
        String content,

        Visibility visibility
) {
}

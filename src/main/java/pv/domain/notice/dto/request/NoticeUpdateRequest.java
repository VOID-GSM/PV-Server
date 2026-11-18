package pv.domain.notice.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// null이 아닌 값만 수정한다. 값을 보내면 공백일 수 없다.
public record NoticeUpdateRequest(
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "제목은 공백일 수 없습니다.")
        String title,

        @Size(max = 5000, message = "내용은 5000자 이하여야 합니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "내용은 공백일 수 없습니다.")
        String content
) {
}

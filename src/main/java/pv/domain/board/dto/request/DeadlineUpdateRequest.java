package pv.domain.board.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// 전부 선택값: null인 필드는 수정하지 않음 (단, 이름을 보낼 때는 공백만 있으면 안 됨)
public record DeadlineUpdateRequest(
        @Size(max = 100) @Pattern(regexp = "(?s).*\\S.*", message = "공백만 입력할 수 없습니다.") String name,
        LocalDate date,
        Long assigneeId
) {
}

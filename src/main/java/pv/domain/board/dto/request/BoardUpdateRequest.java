package pv.domain.board.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

// 전부 선택값: null인 필드는 수정하지 않음 (단, 값을 보낼 때는 공백만 있으면 안 됨)
public record BoardUpdateRequest(
        @Size(max = 100) @Pattern(regexp = "(?s).*\\S.*", message = "공백만 입력할 수 없습니다.") String name,
        @Size(max = 20) @Pattern(regexp = "(?s).*\\S.*", message = "공백만 입력할 수 없습니다.") String color,
        LocalDate startDate,
        LocalDate endDate,
        @Size(max = 10) Set<@Min(0) @Max(365) Integer> alertDays
) {
}

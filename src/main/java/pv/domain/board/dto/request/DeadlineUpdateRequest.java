package pv.domain.board.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

// 전부 선택값: null인 필드는 수정하지 않음 (단, 이름을 보낼 때는 공백만 있으면 안 됨)
// assigneeIds: null이면 기존 담당자 유지, 빈 배열([])이면 담당자 전부 해제, 값이 있으면 그 목록으로 교체
public record DeadlineUpdateRequest(
        @Size(max = 100) @Pattern(regexp = "(?s).*\\S.*", message = "공백만 입력할 수 없습니다.") String name,
        LocalDate date,
        @Size(max = 20) Set<@NotNull Long> assigneeIds,
        @Size(max = 2000) String description
) {
}

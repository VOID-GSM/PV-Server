package pv.domain.notice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pv.domain.member.entity.Major;
import pv.domain.notice.entity.NoticeScope;

import java.util.Set;

// scope별 필수 값
// - ALL: 없음 / BOARD: boardId / ACTIVITY: activityId / TARGET: generationIds + majors (둘 다 필수)
// scope와 상관없는 값은 무시한다.
public record NoticeCreateRequest(
        @NotNull(message = "공지 범위는 필수입니다.")
        NoticeScope scope,

        Long boardId,

        Long activityId,

        @Size(max = 50, message = "기수는 최대 50개까지 지정할 수 있습니다.")
        Set<@NotNull(message = "기수 ID는 비어 있을 수 없습니다.") Long> generationIds,

        @Size(max = 10, message = "전공은 최대 10개까지 지정할 수 있습니다.")
        Set<@NotNull(message = "전공은 비어 있을 수 없습니다.") Major> majors,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 5000, message = "내용은 5000자 이하여야 합니다.")
        String content
) {
}

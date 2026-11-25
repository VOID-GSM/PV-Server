package pv.domain.notification.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import pv.domain.member.entity.Major;

import java.util.Map;

// 기수/전공별 디스코드 역할 ID. 보낸 값으로 전체를 교체한다. (빈 맵이면 모두 해제)
public record DiscordRoleUpdateRequest(
        @NotNull(message = "기수 역할은 필수입니다. (없으면 빈 값)")
        Map<@NotNull Long,
                @NotNull @Pattern(regexp = "^\\d{17,20}$", message = "올바른 디스코드 역할 ID가 아닙니다.") String>
                generationRoleIds,

        @NotNull(message = "전공 역할은 필수입니다. (없으면 빈 값)")
        Map<@NotNull Major,
                @NotNull @Pattern(regexp = "^\\d{17,20}$", message = "올바른 디스코드 역할 ID가 아닙니다.") String>
                majorRoleIds
) {
}

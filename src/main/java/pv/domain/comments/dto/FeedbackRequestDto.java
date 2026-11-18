package pv.domain.comments.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pv.domain.comments.entity.FeedbackRequest;
import pv.domain.comments.entity.TargetType;

import java.time.LocalDateTime;
import java.util.List;

public class FeedbackRequestDto {
    @Getter
    @RequiredArgsConstructor
    public enum Scope {
        MEMBERS("특정 사람"),
        SENIOR_ALL("선배 기수 전체"),
        SENIOR_SAME_MAJOR("전공 선배");

        private final String label;
    }

    public record Create(
            @NotNull TargetType targetType,
            @NotNull Long targetId,
            @NotNull Scope scope,
            List<Long> memberIds,
            @Size(max = 500) String message
    ) {
        @AssertTrue(message = "특정 사람을 선택해야 합니다.")
        public boolean isMembersValid() {
            return scope != Scope.MEMBERS || (memberIds != null && !memberIds.isEmpty());
        }
    }

    public record Response(
            Long id, TargetType targetType, Long targetId,
            Long requesterId, String message, LocalDateTime createdAt
    ) {
        public static Response from(FeedbackRequest r) {
            return new Response(r.getId(), r.getTargetType(), r.getTargetId(),
                    r.getRequesterId(), r.getMessage(), r.getCreatedAt());
        }
    }
}

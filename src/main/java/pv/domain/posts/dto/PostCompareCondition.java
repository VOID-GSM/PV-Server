package pv.domain.posts.dto;

import jakarta.validation.constraints.AssertTrue;

public record PostCompareCondition(
        Long sessionId,
        Long questionId
) {
    @AssertTrue(message = "sessionId와 questionId 중 정확히 하나만 지정하세요.")
    public boolean isExactlyOne() {
        return (sessionId == null) != (questionId == null);
    }
}

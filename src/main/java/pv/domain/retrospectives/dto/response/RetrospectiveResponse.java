package pv.domain.retrospectives.dto.response;

import pv.domain.retrospectives.entity.Retrospective;

public record RetrospectiveResponse(
        Long retrospectiveId,
        Long memberId,
        String memberName,
        String content
) {
    public static RetrospectiveResponse from(Retrospective retrospective, String memberName) {
        return new RetrospectiveResponse(
                retrospective.getId(),
                retrospective.getMemberId(),
                memberName,
                retrospective.getContent()
        );
    }
}
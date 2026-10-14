package pv.domain.board.dto.response;

import java.util.List;

public record BoardDetailResponse(
        BoardSummaryResponse board,
        List<ParticipantItem> participants,
        List<DeadlineResponse> deadlines
) {
    public record ParticipantItem(Long memberId, String name) {
    }
}

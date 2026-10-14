package pv.domain.board.dto.response;

import java.util.List;

public record MilestoneConfirmResponse(
        List<DeadlineResponse> deadlines,
        List<ConflictWarning> warnings
) {
}

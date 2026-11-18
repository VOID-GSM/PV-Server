package pv.domain.board.dto.response;

import java.util.List;

public record DeadlineSaveResponse(
        DeadlineResponse deadline,
        List<ConflictWarning> warnings
) {
}

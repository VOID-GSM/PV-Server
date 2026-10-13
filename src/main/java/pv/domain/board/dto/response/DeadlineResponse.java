package pv.domain.board.dto.response;

import pv.domain.board.entity.Deadline;

import java.time.LocalDate;

public record DeadlineResponse(
        Long id,
        String name,
        LocalDate date,
        Long assigneeId,
        String assigneeName,
        boolean milestone
) {
    public static DeadlineResponse of(Deadline d, String assigneeName) {
        return new DeadlineResponse(d.getId(), d.getName(), d.getDeadlineDate(),
                d.getAssigneeId(), assigneeName, d.isMilestone());
    }
}

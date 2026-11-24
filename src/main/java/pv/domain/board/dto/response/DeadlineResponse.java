package pv.domain.board.dto.response;

import pv.domain.board.entity.Deadline;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DeadlineResponse(
        Long id,
        String name,
        LocalDate date,
        List<Assignee> assignees,
        boolean milestone,
        String description
) {
    public record Assignee(Long memberId, String name) {
    }

    public static DeadlineResponse of(Deadline d, Map<Long, String> names) {
        List<Assignee> assignees = d.getAssigneeIds().stream()
                .map(id -> new Assignee(id, names.get(id)))
                .toList();
        return new DeadlineResponse(d.getId(), d.getName(), d.getDeadlineDate(),
                assignees, d.isMilestone(), d.getDescription());
    }
}

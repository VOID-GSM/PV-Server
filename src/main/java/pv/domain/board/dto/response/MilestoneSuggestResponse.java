package pv.domain.board.dto.response;

import java.time.LocalDate;
import java.util.List;

public record MilestoneSuggestResponse(
        LocalDate finalDeadline,
        List<Item> milestones
) {
    public record Item(String name, LocalDate date) {
    }
}

package pv.domain.board.dto.response;

import pv.domain.board.entity.Board;
import pv.domain.board.entity.BoardStatus;
import pv.domain.board.entity.BoardType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.TreeSet;

public record BoardSummaryResponse(
        Long id,
        String name,
        BoardType type,
        String color,
        LocalDate startDate,
        LocalDate endDate,
        long dDay,
        Set<Integer> alertDays,
        BoardStatus status
) {
    public static BoardSummaryResponse from(Board b) {
        return new BoardSummaryResponse(
                b.getId(), b.getName(), b.getType(), b.getColor(),
                b.getStartDate(), b.getEndDate(),
                ChronoUnit.DAYS.between(LocalDate.now(), b.getEndDate()),
                new TreeSet<>(b.getAlertDays()), b.getStatus());
    }
}

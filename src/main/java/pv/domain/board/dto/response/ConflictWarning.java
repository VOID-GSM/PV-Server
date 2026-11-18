package pv.domain.board.dto.response;

import java.time.LocalDate;

// 같은 담당자의 같은 날짜 마감이 이미 있을 때 함께 반환하는 경고
public record ConflictWarning(
        Long deadlineId,
        Long boardId,
        String boardName,
        String deadlineName,
        LocalDate date
) {
}

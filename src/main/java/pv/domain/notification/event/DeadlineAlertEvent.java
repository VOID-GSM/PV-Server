package pv.domain.notification.event;

import java.time.LocalDate;
import java.util.Set;

public record DeadlineAlertEvent(
        Kind kind,
        Long teamId,
        Long boardId,
        String boardName,
        Long deadlineId,
        String deadlineName,
        LocalDate deadlineDate,
        Integer daysLeft,
        Set<Long> assigneeIds
) {
    public enum Kind {
        ASSIGNED,   // 마감에 담당자로 지정됨
        D_DAY       // 보드 alertDays 기준 D-n 알림
    }
}
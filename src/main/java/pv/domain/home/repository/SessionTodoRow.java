package pv.domain.home.repository;

import java.time.LocalDate;

public record SessionTodoRow(
        Long sessionId,
        String activityName,
        LocalDate sessionDate,
        int topicOpenDaysBefore
) {
}

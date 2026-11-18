package pv.domain.home.dto;

import pv.domain.home.type.TodoType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record HomeTodosResponse(
        LocalDate date,
        int totalCount,
        Map<TodoType, Long> counts,
        List<TodoItemResponse> items
) {
}

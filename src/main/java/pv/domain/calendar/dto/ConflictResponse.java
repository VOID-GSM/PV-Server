package pv.domain.calendar.dto;

import java.time.LocalDate;
import java.util.List;

public record ConflictResponse(
        LocalDate weekStart,
        List<EventResponse> deadlines
) {}
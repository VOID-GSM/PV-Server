package pv.domain.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pv.domain.calendar.entity.EventType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

public record EventCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 2000) String memo,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        boolean allDay,
        EventType type,
        Long boardId,
        @Size(max = 20)Set<@NotNull Long> assigneeIds
        ) {}

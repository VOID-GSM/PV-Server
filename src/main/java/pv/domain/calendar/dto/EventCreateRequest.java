package pv.domain.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EventCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 2000) String memo,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        boolean allDay
) {}
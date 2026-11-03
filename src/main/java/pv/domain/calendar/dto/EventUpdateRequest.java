package pv.domain.calendar.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;

public record EventUpdateRequest(
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "제목은 공백일 수 없습니다.") String title,
        @Size(max = 2000) String memo,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Boolean allDay
) {}
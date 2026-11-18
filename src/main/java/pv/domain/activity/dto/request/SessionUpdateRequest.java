package pv.domain.activity.dto.request;

import jakarta.validation.constraints.Size;
import pv.domain.activity.entity.SessionStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record SessionUpdateRequest(
        @Size(max = 200) String topic,
        LocalDate sessionDate,
        LocalTime sessionTime,
        SessionStatus status
) {
}
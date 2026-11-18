package pv.domain.activity.dto.response;

import pv.domain.activity.entity.ActivitySession;
import pv.domain.activity.entity.SessionStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record SessionResponse(
        Long id, int sessionNo, LocalDate sessionDate, LocalTime sessionTime,
        String topic, SessionStatus status, String statusLabel, boolean votingOpen
) {
    public static SessionResponse of(ActivitySession s, boolean votingOpen) {
        return new SessionResponse(s.getId(), s.getSessionNo(), s.getSessionDate(), s.getSessionTime(),
                s.getTopic(), s.getStatus(), s.getStatus().getLabel(), votingOpen);
    }
}
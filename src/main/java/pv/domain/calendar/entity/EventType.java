package pv.domain.calendar.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
    DEADLINE("보드 마감"),
    MEETING("보드 회의"),
    ACTIVITY("활동"),
    PERSONAL("개인 일정");

    private final String label;
}

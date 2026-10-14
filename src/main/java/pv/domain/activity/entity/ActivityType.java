package pv.domain.activity.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityType {
    STUDY("스터디"),
    INTERVIEW("면접 준비"),
    PORTFOLIO("포폴 피드백");

    private final String label;
}
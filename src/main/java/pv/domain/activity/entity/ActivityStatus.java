package pv.domain.activity.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityStatus {
    RECRUITING("모집 중"),
    ONGOING("진행 중"),
    ENDED("종료");

    private final String label;
}
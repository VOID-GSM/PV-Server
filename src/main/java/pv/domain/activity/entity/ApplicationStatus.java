package pv.domain.activity.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApplicationStatus {
    PENDING("승인 대기"),
    APPROVED("승인"),
    REJECTED("거절");

    private final String label;
}
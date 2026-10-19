package pv.domain.attendance.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbsenceStatus {
    REQUESTED("대기"),
    APPROVED("승인"),
    REJECTED("반려");

    private final String label;
}

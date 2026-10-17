package pv.domain.attendance.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceStatus {
    PENDING("미체크"),
    PRESENT("참석"),
    EXCUSED("승인 불참"),
    ABSENT("무단 불참");

    private final String label;
}

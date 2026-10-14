package pv.domain.activity.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SessionStatus {
    SCHEDULED("예정"),
    CANCELLED("휴강"),
    DONE("완료");

    private final String label;
}
package pv.domain.activity.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityRole {
    LEADER("스터디장"),
    MEMBER("참여자");

    private final String label;
}
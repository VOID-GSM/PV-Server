package pv.domain.activity.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecruitMode {
    FIRST_COME("선착순"),
    APPROVAL("승인제");

    private final String label;
}
package pv.domain.comments.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TargetType {
    NOTE("정리글"),
    SUBMISSION("과제 제출물"),
    WORKLOG("업무일지");

    private final String label;
}

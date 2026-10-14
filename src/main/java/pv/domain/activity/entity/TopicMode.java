package pv.domain.activity.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TopicMode {
    VOTE("투표형"),
    ASSIGNED("지정형");

    private final String label;
}
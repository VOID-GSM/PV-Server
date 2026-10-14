package pv.domain.worklogs.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Visibility {
    PUBLIC("공개"),
    PRIVATE("비공개");

    private final String label;
}

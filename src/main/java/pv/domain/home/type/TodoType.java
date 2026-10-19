package pv.domain.home.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TodoType {
    TOPIC_INPUT("주제 입력"),
    ASSIGNMENT_SUBMIT("과제 제출"),
    FEEDBACK_GIVE("피드백 작성"),
    TOPIC_VOTE("주제 투표"),
    ABSENCE_APPROVE("불참 요청 처리");

    private final String label;
}
package pv.domain.interviews.dto;

import pv.domain.interviews.entity.InterviewCategory;
import pv.domain.interviews.entity.InterviewQuestion;
import pv.domain.interviews.entity.InterviewQuestion;

import java.time.LocalDateTime;

public record QuestionResponse(
        Long id,
        String content,
        InterviewQuestion.Major major,
        InterviewCategory category,
        LocalDateTime createdAt
) {
    public static QuestionResponse from(InterviewQuestion q) {
        return new QuestionResponse(
                q.getId(),
                q.getContent(),
                q.getMajor(),
                q.getCategory(),
                q.getCreatedAt()
        );
    }
}

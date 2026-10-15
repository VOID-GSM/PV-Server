package pv.domain.interviews.dto;

import pv.domain.interviews.entity.InterviewAnswer;

import java.time.LocalDateTime;

public record AnswerResponse(
        Long id,
        Long memberId,
        String memberName,
        String content,
        LocalDateTime createdAt
) {
    public static AnswerResponse from(InterviewAnswer a) {
        return new AnswerResponse(
                a.getId(),
                a.getMember().getId(),
                a.getMember().getName(),
                a.getContent(),
                a.getCreatedAt()
        );
    }
}

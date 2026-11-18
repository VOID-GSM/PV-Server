package pv.domain.interviews.dto;

public record QuestionDetailResponse(
        QuestionResponse question,
        Long answerCount
) {
}

package pv.domain.interviews.dto;

import pv.domain.interviews.entity.InterviewCategory;

import java.util.List;

public record QuestionGroupResponse(
        InterviewCategory category,
        String label,
        List<QuestionResponse> questions
) {
}

package pv.domain.activity.dto.response;

public record TopicCandidateResponse(
        Long id, String title, Long proposerId, String proposerName, long voteCount, boolean voted
) {
}
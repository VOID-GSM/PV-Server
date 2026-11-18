package pv.domain.activity.dto.query;

public record VoteCount(Long sessionId, Long candidateId, long count) {}
package pv.domain.member.dto.response;

import pv.domain.member.entity.Generation;

public record GenerationResponse(Long id, Integer number) {
    public static GenerationResponse from(Generation g) {
        return new GenerationResponse(g.getId(), g.getNumber());
    }
}

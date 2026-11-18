package pv.domain.skills.service;

import java.util.Collection;
import java.util.Map;


public interface AssignmentPassReader {
    Map<Long, Long> countPassedBySkill(Long memberId, Collection<Long> skillIds);
}

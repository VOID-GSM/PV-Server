package pv.domain.calendar.port;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pv.domain.activity.repository.ActivityMemberRepository;
import pv.domain.activity.repository.ActivityRepository;
import pv.domain.board.repository.BoardRepository;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.repository.MemberRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CalendarMembershipAdapter implements CalendarMembershipPort {
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final ActivityMemberRepository activityMemberRepository;
    private final ActivityRepository activityRepository;

    @Override
    public Set<Long> boardIdsOf(Long memberId) {
        return memberRepository.findById(memberId)
                .map(m -> new HashSet<>(boardRepository.findIdsByTeamId(m.getTeamId())))
                .orElseGet(HashSet::new);
    }

    @Override
    public Set<Long> activityIdsOf(Long memberId) {
        return new HashSet<>(activityMemberRepository.findActivityIdsByMemberId(memberId));
    }

    @Override
    public boolean isLeader(Long memberId) {
        return memberRepository.findById(memberId)
                .map(Member::isLeader)
                .orElse(false);
    }

    @Override
    public boolean areJoinedTeammates(Long memberId, Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return true;
        Long teamId = memberRepository.findById(memberId).map(Member::getTeamId).orElse(null);
        if (teamId == null) return false;

        Set<Long> ids = new HashSet<>(memberIds);
        long valid = memberRepository.findAllById(ids).stream()
                .filter(m -> teamId.equals(m.getTeamId()) && m.getStatus() == MemberStatus.JOINED)
                .count();
        return valid == ids.size();
    }

    @Override
    public Set<Long> teamActivityIdsOf(Long memberId) {
        return memberRepository.findById(memberId)
                .map(m -> new HashSet<>(activityRepository.findIdsByTeamId(m.getTeamId())))
                .orElseGet(HashSet::new);
    }
}

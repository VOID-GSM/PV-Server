package pv.domain.attendance.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pv.domain.activity.entity.ActivityRole;
import pv.domain.activity.repository.ActivityMemberRepository;
import pv.domain.member.entity.Member;
import pv.domain.member.repository.MemberRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AttendanceAuthorizer {
    private final MemberRepository memberRepository;
    private final ActivityMemberRepository activityMemberRepository;

    public boolean isManager(Long memberId) {
        if (memberId == null) {
            return false;
        }
        return memberRepository.findById(memberId)
                .map(Member::isLeader)
                .orElse(false);
    }

    public boolean isLeader(Long activityId, Long memberId) {
        if (memberId == null) {
            return false;
        }
        return activityMemberRepository.existsByActivityIdAndMemberIdAndRole(
                activityId, memberId, ActivityRole.LEADER);
    }

    public List<Long> leadingActivityIds(Long memberId) {
        if (memberId == null) {
            return List.of();
        }
        return activityMemberRepository.findActivityIdsByMemberIdAndRole(memberId, ActivityRole.LEADER);
    }

    public boolean isParticipant(Long activityId, Long memberId) {
        if (memberId == null) {
            return false;
        }
        return activityMemberRepository.existsByActivityIdAndMemberId(activityId, memberId);
    }
}
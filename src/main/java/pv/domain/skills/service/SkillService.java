package pv.domain.skills.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.member.entity.Generation;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;
import pv.domain.member.repository.MemberRepository;
import pv.domain.skills.dto.MemberSkillItemResponse;
import pv.domain.skills.dto.MemberSkillsResponse;
import pv.domain.skills.dto.SelfAssessmentRequest;
import pv.domain.skills.dto.SelfAssessmentRequest.ScoreItem;
import pv.domain.skills.dto.SkillCreateRequest;
import pv.domain.skills.dto.SkillResponse;
import pv.domain.skills.dto.SkillUpdateRequest;
import pv.domain.skills.entity.MemberSkill;
import pv.domain.skills.entity.Skill;
import pv.domain.skills.repository.MemberSkillRepository;
import pv.domain.skills.repository.SkillRepository;
import pv.global.exception.CustomException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillService {
    private final SkillRepository skillRepository;
    private final MemberSkillRepository memberSkillRepository;
    private final MemberRepository memberRepository;
    private final AssignmentPassReader assignmentPassReader;

    public List<SkillResponse> getSkills(Major major) {
        List<Skill> skills = (major == null)
                ? skillRepository.findAllByActiveTrueOrderByMajorAscSortOrderAscIdAsc()
                : skillRepository.findAllByMajorAndActiveTrueOrderBySortOrderAscIdAsc(major);
        return skills.stream().map(SkillResponse::from).toList();
    }

    @Transactional
    public SkillResponse createSkill(Long requesterId, SkillCreateRequest request) {
        requireLeader(requesterId);
        String name = request.name().trim();
        if (skillRepository.existsByMajorAndName(request.major(), name)) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 존재하는 스킬입니다.");
        }
        int sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
        Skill skill = skillRepository.save(new Skill(request.major(), name, request.description(), sortOrder));
        return SkillResponse.from(skill);
    }

    @Transactional
    public SkillResponse updateSkill(Long requesterId, Long skillId, SkillUpdateRequest request) {
        requireLeader(requesterId);
        Skill skill = getSkillOrThrow(skillId);

        if (request.name() != null) {
            String name = request.name().trim();
            if (!name.equals(skill.getName()) && skillRepository.existsByMajorAndName(skill.getMajor(), name)) {
                throw new CustomException(HttpStatus.CONFLICT, "이미 존재하는 스킬 이름입니다.");
            }
            skill.rename(name);
        }
        if (request.description() != null) {
            skill.changeDescription(request.description());
        }
        if (request.sortOrder() != null) {
            skill.changeSortOrder(request.sortOrder());
        }
        if (request.active() != null) {
            skill.changeActive(request.active());
        }
        return SkillResponse.from(skill);
    }

    public MemberSkillsResponse getMySkills(Long memberId) {
        return buildMemberSkills(getJoinedMember(memberId));
    }

    @Transactional
    public MemberSkillsResponse assessMySkills(Long memberId, SelfAssessmentRequest request) {
        Member member = getJoinedMember(memberId);
        List<ScoreItem> items = request.scores();

        Set<Long> skillIds = items.stream().map(ScoreItem::skillId).collect(Collectors.toSet());
        if (skillIds.size() != items.size()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "같은 스킬이 중복으로 포함되어 있습니다.");
        }

        Map<Long, Skill> skills = skillRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        if (skills.size() != skillIds.size()) {
            throw new CustomException(HttpStatus.NOT_FOUND, "존재하지 않는 스킬이 포함되어 있습니다.");
        }
        for (Skill skill : skills.values()) {
            if (!skill.isActive()) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "비활성화된 스킬은 평가할 수 없습니다.");
            }
            if (!member.getMajors().contains(skill.getMajor())) {
                throw new CustomException(HttpStatus.FORBIDDEN, "본인 전공의 스킬만 평가할 수 있습니다.");
            }
        }

        Map<Long, MemberSkill> existing = memberSkillRepository
                .findByMemberIdAndSkillIds(memberId, skillIds).stream()
                .collect(Collectors.toMap(ms -> ms.getSkill().getId(), Function.identity()));

        List<MemberSkill> toInsert = new ArrayList<>();
        for (ScoreItem item : items) {
            MemberSkill current = existing.get(item.skillId());
            if (current != null) {
                current.reassess(item.score());   // 변경 감지로 UPDATE
            } else {
                toInsert.add(new MemberSkill(member, skills.get(item.skillId()), item.score()));
            }
        }
        memberSkillRepository.saveAll(toInsert);

        return buildMemberSkills(member);
    }

    public MemberSkillsResponse getMemberSkills(Long requesterId, Long targetMemberId) {
        Member requester = getJoinedMember(requesterId);
        Member target = getMemberOrThrow(targetMemberId);
        Long teamId = requester.getTeamId();

        // 다른 팀 회원은 존재 여부도 노출하지 않음
        if (teamId == null || !teamId.equals(target.getTeamId())) {
            throw new CustomException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }

        boolean allowed = requester.getId().equals(target.getId())
                || requester.isLeader()
                || isSeniorOf(requester, target);
        if (!allowed) {
            throw new CustomException(HttpStatus.FORBIDDEN, "본인, 부장 또는 선배만 조회할 수 있습니다.");
        }
        return buildMemberSkills(target);
    }

    // ===== 공통 =====

    private MemberSkillsResponse buildMemberSkills(Member member) {
        Set<Major> majors = new HashSet<>(member.getMajors());
        if (majors.isEmpty()) {
            return MemberSkillsResponse.of(member, List.of());
        }

        List<Skill> skills = skillRepository.findAllByMajorInAndActiveTrueOrderByMajorAscSortOrderAscIdAsc(majors);
        if (skills.isEmpty()) {
            return MemberSkillsResponse.of(member, List.of());
        }
        List<Long> skillIds = skills.stream().map(Skill::getId).toList();

        Map<Long, MemberSkill> assessments = memberSkillRepository
                .findByMemberIdAndSkillIds(member.getId(), skillIds).stream()
                .collect(Collectors.toMap(ms -> ms.getSkill().getId(), Function.identity()));
        Map<Long, Long> passed = assignmentPassReader.countPassedBySkill(member.getId(), skillIds);

        List<MemberSkillItemResponse> items = skills.stream()
                .map(s -> MemberSkillItemResponse.of(
                        s, assessments.get(s.getId()), passed.getOrDefault(s.getId(), 0L)))
                .toList();
        return MemberSkillsResponse.of(member, items);
    }

    private static boolean isSeniorOf(Member requester, Member target) {
        Generation mine = requester.getGeneration();
        Generation theirs = target.getGeneration();
        return mine != null && theirs != null && mine.getNumber() < theirs.getNumber();
    }

    private void requireLeader(Long memberId) {
        if (!getJoinedMember(memberId).isLeader()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "부장만 사용할 수 있는 기능입니다.");
        }
    }

    private Member getJoinedMember(Long memberId) {
        Member member = getMemberOrThrow(memberId);
        if (!member.isJoined()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "가입이 완료된 팀원만 사용할 수 있습니다.");
        }
        return member;
    }

    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }

    private Skill getSkillOrThrow(Long skillId) {
        return skillRepository.findById(skillId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "스킬을 찾을 수 없습니다."));
    }
}
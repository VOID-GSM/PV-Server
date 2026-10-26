package pv.domain.skills.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.member.entity.Major;
import pv.domain.skills.dto.MemberSkillsResponse;
import pv.domain.skills.dto.SelfAssessmentRequest;
import pv.domain.skills.dto.SkillCreateRequest;
import pv.domain.skills.dto.SkillResponse;
import pv.domain.skills.dto.SkillUpdateRequest;
import pv.domain.skills.service.SkillService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillController {
    private final SkillService skillService;

    @GetMapping
    public List<SkillResponse> getSkills(@RequestParam(required = false) Major major) {
        return skillService.getSkills(major);
    }

    @PostMapping
    public ResponseEntity<SkillResponse> createSkill(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SkillCreateRequest request) {
        SkillResponse response = skillService.createSkill(memberId, request);
        return ResponseEntity.created(URI.create("/skills/" + response.id())).body(response);
    }

    @PatchMapping("/{skillId:\\d+}")
    public SkillResponse updateSkill(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long skillId,
            @Valid @RequestBody SkillUpdateRequest request) {
        return skillService.updateSkill(memberId, skillId, request);
    }

    @GetMapping("/me")
    public MemberSkillsResponse getMySkills(@AuthenticationPrincipal Long memberId) {
        return skillService.getMySkills(memberId);
    }

    @PatchMapping("/me")
    public MemberSkillsResponse assessMySkills(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SelfAssessmentRequest request) {
        return skillService.assessMySkills(memberId, request);
    }

    @GetMapping("/members/{memberId}")
    public MemberSkillsResponse getMemberSkills(
            @AuthenticationPrincipal Long requesterId,
            @PathVariable Long memberId) {
        return skillService.getMemberSkills(requesterId, memberId);
    }
}
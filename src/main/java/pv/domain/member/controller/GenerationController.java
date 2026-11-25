package pv.domain.member.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.member.dto.response.GenerationResponse;
import pv.domain.member.service.MemberService;

import java.util.List;

@RestController
@RequestMapping("/generations")
@RequiredArgsConstructor
public class GenerationController {

    private final MemberService memberService;

    // 우리 팀 기수 목록 (가입 완료 팀원 누구나). 기수 번호 오름차순
    @GetMapping
    public ResponseEntity<List<GenerationResponse>> getGenerations(
            @AuthenticationPrincipal Long loginMemberId) {
        return ResponseEntity.ok(memberService.getGenerations(loginMemberId));
    }
}

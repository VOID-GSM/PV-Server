package pv.domain.interviews.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pv.domain.interviews.dto.GroupRequest;
import pv.domain.interviews.dto.GroupResponse;
import pv.domain.interviews.dto.QuestionCreateRequest;
import pv.domain.interviews.dto.QuestionResponse;
import pv.domain.interviews.service.InterviewGroupService;
import pv.domain.member.entity.Member;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/interviews/groups")
@RequiredArgsConstructor
public class InterviewGroupController {
    private final InterviewGroupService groupService;

    @PostMapping
    public ResponseEntity<Void> create(
            @Valid @RequestBody GroupRequest req,
            @AuthenticationPrincipal Long memberId
    ) {
        Long id = groupService.create(req, memberId);
        return ResponseEntity.created(URI.create("/interviews/groups/" + id)).build();
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getGroups(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(groupService.getGroups(memberId));
    }

    @PatchMapping("/{groupId}")
    public ResponseEntity<Void> rename(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupRequest req,
            @AuthenticationPrincipal Long memberId
    ) {
        groupService.rename(groupId, req, memberId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Long memberId
    ) {
        groupService.delete(groupId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/questions")
    public ResponseEntity<Void> createQuestion(
            @PathVariable Long groupId,
            @Valid @RequestBody QuestionCreateRequest req,
            @AuthenticationPrincipal Long memberId
    ) {
        Long id = groupService.createQuestion(groupId, req, memberId);
        return ResponseEntity.created(URI.create("/interviews/questions/" + id)).build();
    }

    @GetMapping("/{groupId}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(groupService.getQuestions(groupId, memberId));
    }
}

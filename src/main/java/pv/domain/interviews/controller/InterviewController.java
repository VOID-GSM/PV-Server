package pv.domain.interviews.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pv.domain.interviews.dto.*;
import pv.domain.interviews.entity.InterviewCategory;
import pv.domain.interviews.service.InterviewService;
import pv.domain.member.entity.Major;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/interviews/questions")
@RequiredArgsConstructor
public class InterviewController {
    private final InterviewService interviewService;

    @GetMapping
    public ResponseEntity<List<QuestionResponse>> getQuestions(
            @RequestParam(required = false) Major major,
            @RequestParam(required = false) InterviewCategory category,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(interviewService.getQuestions(major, category, memberId));
    }

    @GetMapping("/random")
    public ResponseEntity<List<QuestionResponse>> getRandom(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Major major,
            @RequestParam(required = false) InterviewCategory category,
            @RequestParam(defaultValue = "1") @Min(1) @Max(20) int count,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(interviewService.getRandom(groupId, major, category, count, memberId));
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionDetailResponse> getQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(interviewService.getQuestion(questionId, memberId));
    }

    @GetMapping("/{questionId}/answers")
    public ResponseEntity<List<AnswerResponse>> getAnswers(
            @PathVariable Long questionId,
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(interviewService.getAnswers(questionId, memberId));
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long questionId,
            @AuthenticationPrincipal Long memberId
    ) {
        interviewService.delete(questionId, memberId);
        return ResponseEntity.noContent().build();
    }
}

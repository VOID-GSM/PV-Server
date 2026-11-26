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
    public List<QuestionResponse> list(
            @RequestParam(required = false) Major major,
            @RequestParam(required = false) InterviewCategory category
    ) {
        return interviewService.getQuestions(major, category);
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid QuestionCreateRequest req
    ) {
        Long id = interviewService.create(memberId, req);
        return ResponseEntity.created(URI.create("/interviews/questions/" + id)).build();
    }

    @GetMapping("/random")
    public List<QuestionResponse> random(
            @RequestParam(required = false) Major major,
            @RequestParam(required = false) InterviewCategory category,
            @RequestParam(defaultValue = "1") @Min(1) @Max(20) int count
    ) {
        return interviewService.getRandom(major, category, count);
    }

    @GetMapping("/{questionId}")
    public QuestionDetailResponse detail(@PathVariable Long questionId) {
        return interviewService.getQuestion(questionId);
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long questionId) {
        interviewService.delete(memberId, questionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{questionId}/answers")
    public List<AnswerResponse> answers(@PathVariable Long questionId) {
        return interviewService.getAnswers(questionId);
    }

    @PostMapping("/{questionId}/answers")
    public ResponseEntity<AnswerResponse> createAnswer(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long questionId,
            @RequestBody @Valid AnswerRequest req
    ) {
        AnswerResponse response = interviewService.createAnswer(memberId, questionId, req);
        return ResponseEntity
                .created(URI.create("/interviews/questions/" + questionId + "/answers"))
                .body(response);
    }

    @PatchMapping("/{questionId}/answers/{answerId}")
    public ResponseEntity<Void> updateAnswer(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long questionId,
            @PathVariable Long answerId,
            @RequestBody @Valid AnswerRequest req
    ) {
        interviewService.updateAnswer(memberId, questionId, answerId, req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups")
    public List<QuestionGroupResponse> groups(@RequestParam(required = false) Major major) {
        return interviewService.getGroupedQuestions(major);
    }
}

package pv.domain.interviews.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.interviews.dto.AnswerRequest;
import pv.domain.interviews.dto.AnswerResponse;
import pv.domain.interviews.dto.QuestionCreateRequest;
import pv.domain.interviews.dto.QuestionDetailResponse;
import pv.domain.interviews.dto.QuestionResponse;
import pv.domain.interviews.entity.InterviewCategory;
import pv.domain.interviews.entity.InterviewQuestion;
import pv.domain.interviews.service.InterviewService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/interviews/questions")
@RequiredArgsConstructor
public class InterviewController {
    private final InterviewService interviewService;

    @GetMapping
    public List<QuestionResponse> list(
            @RequestParam(required = false) InterviewQuestion.Major major,
            @RequestParam(required = false) InterviewCategory category
    ) {
        return interviewService.getQuestions(major, category);
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @RequestBody @Valid QuestionCreateRequest req
    ) {
        return ResponseEntity.created(URI.create("/interviews/questions/" + interviewService.create(req))).build();
    }

    @GetMapping("/random")
    public List<QuestionResponse> create(
            @RequestParam(required = false) InterviewQuestion.Major major,
            @RequestParam(required = false) InterviewCategory category,
            @RequestParam(defaultValue = "1") @Min(1) @Max(20) int count
    ) {
        return interviewService.getRandom(major, category, count);
    }

    @GetMapping("/{questionId}")
    public QuestionDetailResponse detail(@PathVariable Long questionId) {
        return interviewService.getQuestion(questionId);
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
                    .created(URI.create("/interviews/questions/" + questionId + "/answers/" + response.id()))
                    .body(response);
    }
}

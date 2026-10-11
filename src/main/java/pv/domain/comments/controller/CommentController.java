package pv.domain.comments.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.comments.dto.CommentDto;
import pv.domain.comments.dto.FeedbackRequestDto;
import pv.domain.comments.entity.TargetType;
import pv.domain.comments.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentDto.Response> getComments(@RequestParam TargetType targetType,
                                                 @RequestParam Long targetId) {
        return commentService.getComments(targetType, targetId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto.Response create(@AuthenticationPrincipal Long memberId,
                                      @RequestBody @Valid CommentDto.Create req) {
        return commentService.create(memberId, req);
    }

    @PatchMapping("/{commentId}")
    public CommentDto.Response update(@AuthenticationPrincipal Long memberId,
                                      @PathVariable Long commentId,
                                      @RequestBody @Valid CommentDto.Update req) {
        return commentService.update(memberId, commentId, req);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long memberId,
                       @PathVariable Long commentId) {
        commentService.delete(memberId, commentId);
    }

    // Member 머지 후 피드백 요청 생성 API 추가 (POST /comments/feedback-requests)

    @GetMapping("/feedback-requests/me")
    public List<FeedbackRequestDto.Response> getMyRequests(@AuthenticationPrincipal Long memberId) {
        return commentService.getMyRequests(memberId);
    }

    @PostMapping("/feedback-requests/{requestId}/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@AuthenticationPrincipal Long memberId,
                         @PathVariable Long requestId) {
        commentService.complete(memberId, requestId);
    }
}
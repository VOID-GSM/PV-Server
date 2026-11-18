package pv.domain.retrospectives.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.retrospectives.dto.response.RetrospectiveResponse;
import pv.domain.retrospectives.dto.request.RetrospectiveUpdateRequest;
import pv.domain.retrospectives.service.RetrospectiveService;

import java.util.List;

@RestController
@RequestMapping("/retrospectives")
@RequiredArgsConstructor
public class RetrospectiveController {
    private final RetrospectiveService retrospectiveService;

    @GetMapping("/boards/{boardId}")
    public List<RetrospectiveResponse> getByBoard(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long boardId
    ) {
        return retrospectiveService.getByBoard(memberId, boardId);
    }

    @PatchMapping("/boards/{boardId}/me")
    public ResponseEntity<Void> updateMine(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long boardId,
            @RequestBody @Valid RetrospectiveUpdateRequest request
    ) {
        retrospectiveService.updateMine(memberId, boardId, request);
        return ResponseEntity.noContent().build();
    }
}
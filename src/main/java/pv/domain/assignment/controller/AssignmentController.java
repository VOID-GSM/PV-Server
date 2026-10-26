package pv.domain.assignment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pv.domain.assignment.dto.projection.*;
import pv.domain.assignment.dto.request.*;
import pv.domain.assignment.dto.response.*;
import pv.domain.assignment.service.AssignmentService;
import pv.domain.assignment.service.SubmissionService;

import java.net.URI;

@RestController
@RequestMapping("/assignments")
@RequiredArgsConstructor
public class AssignmentController {
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    @GetMapping
    public PageResponse<AssignmentSummaryResponse> list(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(defaultValue = "RECEIVED") AssignmentListType type,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return assignmentService.list(memberId, type, pageable);
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid AssignmentCreateRequest req
    ) {
        Long id = assignmentService.create(memberId, req);
        return ResponseEntity.created(URI.create("/assignments/" + id)).build();
    }

    @GetMapping("/{assignmentId}")
    public AssignmentDetailResponse detail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long assignmentId
    ) {
        return assignmentService.get(memberId, assignmentId);
    }

    @PatchMapping("/{assignmentId}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long assignmentId,
            @RequestBody @Valid AssignmentUpdateRequest req
    ) {
        assignmentService.update(memberId, assignmentId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long assignmentId
    ) {
        assignmentService.delete(memberId, assignmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{assignmentId}/submissions")
    public SubmissionStatusResponse submissions(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long assignmentId
    ) {
        return submissionService.getStatus(memberId, assignmentId);
    }

    @PostMapping("/{assignmentId}/submissions")
    public ResponseEntity<SubmissionResponse> submit(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long assignmentId,
            @RequestBody @Valid SubmissionRequest req
    ) {
        SubmissionResponse response = submissionService.submit(memberId, assignmentId, req);
        return ResponseEntity
                .created(URI.create("/assignments/" + assignmentId + "/submissions/" + response.id()))
                .body(response);
    }

    @GetMapping("/{assignmentId}/submissions/{submissionId}")
    public SubmissionResponse submission(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId
    ) {
        return submissionService.get(memberId, assignmentId, submissionId);
    }

    @PatchMapping("/{assignmentId}/submissions/{submissionId}")
    public ResponseEntity<Void> resubmit(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @RequestBody @Valid SubmissionRequest req
    ) {
        submissionService.resubmit(memberId, assignmentId, submissionId, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{assignmentId}/submissions/{submissionId}/pass")
    public ResponseEntity<Void> pass(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @RequestBody(required = false) @Valid ReviewRequest req
    ) {
        submissionService.pass(memberId, assignmentId, submissionId, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{assignmentId}/submissions/{submissionId}/request-resubmission")
    public ResponseEntity<Void> requestResubmission(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @RequestBody @Valid ResubmissionRequest req
    ) {
        submissionService.requestResubmission(memberId, assignmentId, submissionId, req);
        return ResponseEntity.noContent().build();
    }
}
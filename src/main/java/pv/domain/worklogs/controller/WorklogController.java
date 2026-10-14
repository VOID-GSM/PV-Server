package pv.domain.worklogs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.worklogs.dto.WorklogCreateRequest;
import pv.domain.worklogs.dto.WorklogResponse;
import pv.domain.worklogs.dto.WorklogUpdateRequest;
import pv.domain.worklogs.service.WorklogService;

import java.net.URI;

@RestController
@RequestMapping("/worklogs")
@RequiredArgsConstructor
public class WorklogController {
    private final WorklogService worklogService;

    @GetMapping
    public Page<WorklogResponse> getList(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Long boardId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return worklogService.getList(memberId, boardId, pageable);
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid WorklogCreateRequest request
    ) {
        Long id = worklogService.create(memberId, request);
        return ResponseEntity.created(URI.create("/worklogs/" + id)).build();
    }

    @GetMapping("/{worklogId}")
    public WorklogResponse get(@AuthenticationPrincipal Long memberId, @PathVariable Long worklogId) {
        return worklogService.get(memberId, worklogId);
    }

    @PatchMapping("/{worklogId}")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long worklogId,
            @RequestBody @Valid WorklogUpdateRequest request
    ) {
        worklogService.update(memberId, worklogId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{worklogId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long worklogId
    ) {
        worklogService.delete(memberId, worklogId);
        return ResponseEntity.noContent().build();
    }
}

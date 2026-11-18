package pv.domain.board.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pv.domain.board.dto.request.*;
import pv.domain.board.dto.response.*;
import pv.domain.board.service.BoardService;

import java.util.List;

@RequestMapping("/boards")
@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // 보드 목록 조회 (부장)
    @GetMapping
    public ResponseEntity<List<BoardSummaryResponse>> getBoards(
            @AuthenticationPrincipal Long loginMemberId) {
        return ResponseEntity.ok(boardService.getBoards(loginMemberId));
    }

    // 보드 생성 (부장)
    @PostMapping
    public ResponseEntity<BoardSummaryResponse> createBoard(
            @AuthenticationPrincipal Long loginMemberId,
            @Valid @RequestBody BoardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.createBoard(loginMemberId, request));
    }

    // 보드 상세 조회 (부장)
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardDetailResponse> getBoard(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long boardId) {
        return ResponseEntity.ok(boardService.getBoard(loginMemberId, boardId));
    }

    // 보드 수정 (부장)
    @PatchMapping("/{boardId}")
    public ResponseEntity<Void> updateBoard(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long boardId,
            @Valid @RequestBody BoardUpdateRequest request) {
        boardService.updateBoard(loginMemberId, boardId, request);
        return ResponseEntity.noContent().build();
    }

    // 보드 삭제 (부장)
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long boardId) {
        boardService.deleteBoard(loginMemberId, boardId);
        return ResponseEntity.noContent().build();
    }

    // 보드 종료 + 참여자별 회고 자동 생성 (부장)
    @PostMapping("/{boardId}/close")
    public ResponseEntity<Void> closeBoard(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long boardId) {
        boardService.closeBoard(loginMemberId, boardId);
        return ResponseEntity.noContent().build();
    }

    // 마감 목록 조회 - 날짜순 (부장)
    @GetMapping("/{boardId}/deadlines")
    public ResponseEntity<List<DeadlineResponse>> getDeadlines(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long boardId) {
        return ResponseEntity.ok(boardService.getDeadlines(loginMemberId, boardId));
    }

    // 마감 생성 - 담당자 마감이 겹치면 경고 포함 (부장)
    @PostMapping("/{boardId}/deadlines")
    public ResponseEntity<DeadlineSaveResponse> createDeadline(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long boardId,
            @Valid @RequestBody DeadlineCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.createDeadline(loginMemberId, boardId, request));
    }

    // 마감 수정 (부장)
    @PatchMapping("/{boardId}/deadlines/{deadlineId}")
    public ResponseEntity<DeadlineSaveResponse> updateDeadline(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long boardId,
            @PathVariable Long deadlineId,
            @Valid @RequestBody DeadlineUpdateRequest request) {
        return ResponseEntity.ok(boardService.updateDeadline(loginMemberId, boardId, deadlineId, request));
    }

    // 마감 삭제 (부장)
    @DeleteMapping("/{boardId}/deadlines/{deadlineId}")
    public ResponseEntity<Void> deleteDeadline(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long boardId,
            @PathVariable Long deadlineId) {
        boardService.deleteDeadline(loginMemberId, boardId, deadlineId);
        return ResponseEntity.noContent().build();
    }

    // 마일스톤 역산 제안 - 계산만 하고 저장하지 않음 (부장)
    @PostMapping("/{boardId}/milestones/suggest")
    public ResponseEntity<MilestoneSuggestResponse> suggestMilestones(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long boardId,
            @Valid @RequestBody MilestoneSuggestRequest request) {
        return ResponseEntity.ok(boardService.suggestMilestones(loginMemberId, boardId, request));
    }

    // 마일스톤 확정 - 한 번에 저장 + 다른 보드 마감과 겹침 검사 (부장)
    @PostMapping("/{boardId}/milestones")
    public ResponseEntity<MilestoneConfirmResponse> confirmMilestones(
            @AuthenticationPrincipal Long loginMemberId,
            @PathVariable Long boardId,
            @Valid @RequestBody MilestoneConfirmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.confirmMilestones(loginMemberId, boardId, request));
    }
}

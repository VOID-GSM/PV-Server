package pv.domain.board.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pv.domain.board.dto.request.*;
import pv.domain.board.dto.response.*;
import pv.domain.board.entity.Board;
import pv.domain.board.entity.Deadline;
import pv.domain.retrospectives.entity.Retrospective;
import pv.domain.board.repository.BoardRepository;
import pv.domain.board.repository.DeadlineRepository;
import pv.domain.retrospectives.repository.RetrospectiveRepository;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.repository.MemberRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    // 최종 마감 사이에 제안할 중간 마감 개수
    private static final int MILESTONE_COUNT = 3;

    private final BoardRepository boardRepository;
    private final DeadlineRepository deadlineRepository;
    private final RetrospectiveRepository retrospectiveRepository;
    private final MemberRepository memberRepository;

    // ===== 보드 =====

    public List<BoardSummaryResponse> getBoards(Long loginMemberId) {
        Member leader = getLeader(loginMemberId);
        return boardRepository.findAllByTeamIdOrderByEndDateAsc(leader.getTeamId())
                .stream()
                .map(BoardSummaryResponse::from)
                .toList();
    }

    @Transactional
    public BoardSummaryResponse createBoard(Long loginMemberId, BoardCreateRequest request) {
        Member leader = getLeader(loginMemberId);
        validatePeriod(request.startDate(), request.endDate());

        Board board = boardRepository.save(new Board(
                leader.getTeamId(), request.name(), request.type(), request.color(),
                request.startDate(), request.endDate(), request.alertDays()));
        return BoardSummaryResponse.from(board);
    }

    public BoardDetailResponse getBoard(Long loginMemberId, Long boardId) {
        Board board = getBoardOf(loginMemberId, boardId);
        List<Deadline> deadlines = deadlineRepository.findAllByBoardIdOrderByDeadlineDateAscIdAsc(boardId);

        // 참여자 = 이 보드 마감의 담당자들 (중복 제거). 이름은 한 번만 조회해서 재사용
        Set<Long> participantIds = assigneeIds(deadlines);
        Map<Long, String> names = memberNames(participantIds);
        List<BoardDetailResponse.ParticipantItem> participants = participantIds.stream()
                .map(id -> new BoardDetailResponse.ParticipantItem(id, names.get(id)))
                .toList();

        return new BoardDetailResponse(BoardSummaryResponse.from(board), participants, toResponses(deadlines, names));
    }

    @Transactional
    public void updateBoard(Long loginMemberId, Long boardId, BoardUpdateRequest request) {
        Board board = getBoardForUpdate(getLeader(loginMemberId), boardId);
        validateActive(board);

        board.update(request.name(), request.color(), request.startDate(), request.endDate(), request.alertDays());
        validatePeriod(board.getStartDate(), board.getEndDate());
    }

    // 보드와 하위 마감(+회고)을 함께 삭제
    @Transactional
    public void deleteBoard(Long loginMemberId, Long boardId) {
        Board board = getBoardForUpdate(getLeader(loginMemberId), boardId);
        retrospectiveRepository.deleteAllByBoardId(boardId);
        deadlineRepository.deleteAllByBoardId(boardId);
        boardRepository.delete(board);
    }

    // 보드 종료 + 참여자별 회고 자동 생성 (한 트랜잭션, 보드 행 잠금으로 중복 종료/동시 마감 추가 방지)
    @Transactional
    public void closeBoard(Long loginMemberId, Long boardId) {
        Board board = getBoardForUpdate(getLeader(loginMemberId), boardId);
        validateActive(board);

        board.close();

        List<Deadline> deadlines = deadlineRepository.findAllByBoardIdOrderByDeadlineDateAscIdAsc(boardId);
        List<Retrospective> retrospectives = assigneeIds(deadlines).stream()
                .map(memberId -> new Retrospective(boardId, memberId))
                .toList();
        retrospectiveRepository.saveAll(retrospectives);
    }

    // ===== 마감 =====

    public List<DeadlineResponse> getDeadlines(Long loginMemberId, Long boardId) {
        getBoardOf(loginMemberId, boardId);
        return toResponses(deadlineRepository.findAllByBoardIdOrderByDeadlineDateAscIdAsc(boardId));
    }

    // 겹쳐도 생성은 성공하고, 경고를 응답에 함께 반환
    @Transactional
    public DeadlineSaveResponse createDeadline(Long loginMemberId, Long boardId, DeadlineCreateRequest request) {
        Member leader = getLeader(loginMemberId);
        Board board = getBoardForUpdate(leader, boardId);
        validateActive(board);
        validateAssignee(leader, request.assigneeId());

        Deadline deadline = deadlineRepository.save(
                new Deadline(boardId, request.name(), request.date(), request.assigneeId(), false));

        List<ConflictWarning> warnings = findConflicts(List.of(deadline), null);
        return new DeadlineSaveResponse(toResponses(List.of(deadline)).get(0), warnings);
    }

    @Transactional
    public DeadlineSaveResponse updateDeadline(Long loginMemberId, Long boardId, Long deadlineId,
                                               DeadlineUpdateRequest request) {
        Member leader = getLeader(loginMemberId);
        Board board = getBoardForUpdate(leader, boardId);
        validateActive(board);
        validateAssignee(leader, request.assigneeId());

        Deadline deadline = findDeadline(deadlineId, boardId);
        deadline.update(request.name(), request.date(), request.assigneeId());

        List<ConflictWarning> warnings = findConflicts(List.of(deadline), null);
        return new DeadlineSaveResponse(toResponses(List.of(deadline)).get(0), warnings);
    }

    @Transactional
    public void deleteDeadline(Long loginMemberId, Long boardId, Long deadlineId) {
        Board board = getBoardForUpdate(getLeader(loginMemberId), boardId);
        validateActive(board);
        deadlineRepository.delete(findDeadline(deadlineId, boardId));
    }

    // ===== 마일스톤 =====

    // 저장하지 않고 계산만 한다: (시작일 ~ 최종 마감) 구간을 균등 분할
    public MilestoneSuggestResponse suggestMilestones(Long loginMemberId, Long boardId,
                                                      MilestoneSuggestRequest request) {
        Board board = getBoardOf(loginMemberId, boardId);
        validateActive(board);

        LocalDate today = LocalDate.now();
        LocalDate start = board.getStartDate().isAfter(today) ? board.getStartDate() : today;
        LocalDate end = request.finalDeadline();

        long totalDays = ChronoUnit.DAYS.between(start, end);
        if (totalDays < 2) {
            throw error(HttpStatus.BAD_REQUEST, "최종 마감까지 기간이 너무 짧아 중간 마감을 만들 수 없습니다.");
        }

        List<MilestoneSuggestResponse.Item> items = new ArrayList<>();
        Set<LocalDate> used = new HashSet<>();
        for (int i = 1; i <= MILESTONE_COUNT; i++) {
            LocalDate date = start.plusDays(totalDays * i / (MILESTONE_COUNT + 1));
            // 기간이 짧아 날짜가 겹치거나 시작/최종일과 같아지면 건너뜀
            if (date.equals(start) || date.equals(end) || !used.add(date)) continue;
            items.add(new MilestoneSuggestResponse.Item("중간 마감 " + (items.size() + 1), date));
        }
        return new MilestoneSuggestResponse(end, items);
    }

    // 수정된 마일스톤을 한 번에 저장 (전부 성공하거나 전부 실패)
    @Transactional
    public MilestoneConfirmResponse confirmMilestones(Long loginMemberId, Long boardId,
                                                      MilestoneConfirmRequest request) {
        Member leader = getLeader(loginMemberId);
        Board board = getBoardForUpdate(leader, boardId);
        validateActive(board);
        validateAssignees(leader, request.milestones().stream()
                .map(MilestoneConfirmRequest.Item::assigneeId)
                .toList());

        List<Deadline> saved = deadlineRepository.saveAll(request.milestones().stream()
                .map(m -> new Deadline(boardId, m.name(), m.date(), m.assigneeId(), true))
                .toList());

        // 다른 보드 마감과 겹치는지 검사 (겹쳐도 저장은 유지, 경고로 반환)
        return new MilestoneConfirmResponse(toResponses(saved), findConflicts(saved, boardId));
    }

    // ===== 공통 =====

    private Member getLeader(Long loginMemberId) {
        Member member = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다."));
        if (!member.isLeader()) {
            throw error(HttpStatus.FORBIDDEN, "부장만 사용할 수 있는 기능입니다.");
        }
        return member;
    }

    private Board getBoardOf(Long loginMemberId, Long boardId) {
        Member leader = getLeader(loginMemberId);
        return boardRepository.findById(boardId)
                .filter(b -> b.getTeamId().equals(leader.getTeamId()))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "존재하지 않는 보드입니다."));
    }

    // 변경 작업용: 보드 행을 잠그고 조회 (다른 팀의 보드는 존재하지 않는 것처럼 404)
    private Board getBoardForUpdate(Member leader, Long boardId) {
        return boardRepository.findWithLockById(boardId)
                .filter(b -> b.getTeamId().equals(leader.getTeamId()))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "존재하지 않는 보드입니다."));
    }

    private Deadline findDeadline(Long deadlineId, Long boardId) {
        return deadlineRepository.findByIdAndBoardId(deadlineId, boardId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "존재하지 않는 마감입니다."));
    }

    private void validateActive(Board board) {
        if (board.isClosed()) {
            throw error(HttpStatus.CONFLICT, "종료된 보드입니다.");
        }
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw error(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    private void validateAssignee(Member leader, Long assigneeId) {
        validateAssignees(leader, Collections.singletonList(assigneeId));
    }

    // 담당자는 같은 팀의 가입 완료(JOINED) 멤버여야 함. 여러 명이어도 한 번의 쿼리로 검증
    private void validateAssignees(Member leader, Collection<Long> assigneeIds) {
        Set<Long> ids = assigneeIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return;

        long valid = memberRepository.findAllById(ids).stream()
                .filter(m -> leader.getTeamId().equals(m.getTeamId()) && m.getStatus() == MemberStatus.JOINED)
                .count();
        if (valid != ids.size()) {
            throw error(HttpStatus.BAD_REQUEST, "존재하지 않는 담당자입니다.");
        }
    }

    // 대상 마감과 같은 담당자·같은 날짜에 이미 잡힌 다른 마감 찾기 (종료된 보드의 마감은 제외)
    // excludeBoardId가 있으면 해당 보드의 마감은 검사 대상에서 제외
    private List<ConflictWarning> findConflicts(List<Deadline> targets, Long excludeBoardId) {
        Map<Long, Set<LocalDate>> datesByAssignee = new HashMap<>();
        for (Deadline t : targets) {
            if (t.getAssigneeId() == null) continue;
            datesByAssignee.computeIfAbsent(t.getAssigneeId(), k -> new HashSet<>()).add(t.getDeadlineDate());
        }
        if (datesByAssignee.isEmpty()) return List.of();

        Set<Long> targetIds = targets.stream().map(Deadline::getId).collect(Collectors.toSet());
        List<Deadline> found = new ArrayList<>();
        datesByAssignee.forEach((assigneeId, dates) ->
                deadlineRepository.findAllByAssigneeIdAndDeadlineDateIn(assigneeId, dates).stream()
                        .filter(d -> !targetIds.contains(d.getId()))
                        .filter(d -> excludeBoardId == null || !d.getBoardId().equals(excludeBoardId))
                        .forEach(found::add));
        if (found.isEmpty()) return List.of();

        Map<Long, Board> boards = new HashMap<>();
        boardRepository.findAllById(found.stream().map(Deadline::getBoardId).collect(Collectors.toSet()))
                .forEach(b -> boards.put(b.getId(), b));

        return found.stream()
                .filter(d -> {
                    Board b = boards.get(d.getBoardId());
                    return b != null && !b.isClosed();
                })
                .sorted(Comparator.comparing(Deadline::getDeadlineDate).thenComparing(Deadline::getId))
                .map(d -> new ConflictWarning(d.getId(), d.getBoardId(),
                        boards.get(d.getBoardId()).getName(), d.getName(), d.getDeadlineDate()))
                .toList();
    }

    private Set<Long> assigneeIds(List<Deadline> deadlines) {
        return deadlines.stream()
                .map(Deadline::getAssigneeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Collectors.toMap은 value가 null이면 NPE가 나므로 직접 담는다 (이름이 null인 멤버 대비)
    private Map<Long, String> memberNames(Set<Long> memberIds) {
        Map<Long, String> names = new HashMap<>();
        if (memberIds.isEmpty()) return names;
        memberRepository.findAllById(memberIds).forEach(m -> names.put(m.getId(), m.getName()));
        return names;
    }

    private List<DeadlineResponse> toResponses(List<Deadline> deadlines) {
        return toResponses(deadlines, memberNames(assigneeIds(deadlines)));
    }

    private List<DeadlineResponse> toResponses(List<Deadline> deadlines, Map<Long, String> names) {
        return deadlines.stream()
                .map(d -> DeadlineResponse.of(d, d.getAssigneeId() == null ? null : names.get(d.getAssigneeId())))
                .toList();
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }
}

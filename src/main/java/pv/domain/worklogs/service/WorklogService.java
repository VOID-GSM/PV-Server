package pv.domain.worklogs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pv.domain.board.entity.Board;
import pv.domain.board.repository.BoardRepository;
import pv.domain.member.repository.MemberRepository;
import pv.domain.worklogs.dto.WorklogCreateRequest;
import pv.domain.worklogs.dto.WorklogResponse;
import pv.domain.worklogs.dto.WorklogUpdateRequest;
import pv.domain.worklogs.entity.Visibility;
import pv.domain.worklogs.entity.Worklog;
import pv.domain.worklogs.repository.WorklogRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklogService {
    private final WorklogRepository worklogRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;

    public Page<WorklogResponse> getList(Long memberId, Long boardId, Pageable pageable) {
        return worklogRepository.findVisible(memberId, boardId, Visibility.PUBLIC, pageable)
                .map(WorklogResponse::from);
    }

    @Transactional
    public Long create(Long memberId, WorklogCreateRequest request) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 회원입니다.");
        }
        Board board = request.boardId() == null ? null
                : boardRepository.findById(request.boardId())
                  .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "보드를 찾을 수 없습니다."));
        Worklog worklog = Worklog.builder()
                .author(memberRepository.getReferenceById(memberId))
                .board(board)
                .title(request.title())
                .content(request.content())
                .visibility(request.visibility())
                .build();
        return worklogRepository.save(worklog).getId();
    }

    public WorklogResponse get(Long memberId, Long worklogId) {
        return WorklogResponse.from(findVisible(memberId, worklogId));
    }

    @Transactional
    public void update(Long memberId, Long worklogId, WorklogUpdateRequest request) {
        findOwned(memberId, worklogId).update(request.title(), request.content(), request.visibility());
    }

    @Transactional
    public void delete(Long memberId, Long worklogId) {
        worklogRepository.delete(findOwned(memberId, worklogId));
    }

    private Worklog findVisible(Long memberId, Long worklogId) {
        return worklogRepository.findWithAuthorById(worklogId)
                .filter(w -> w.isVisibleTo(memberId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "업무일지를 찾을 수 없습니다."));
    }

    private Worklog findOwned(Long memberId, Long worklogId) {
        Worklog worklog = findVisible(memberId, worklogId);
        if (!worklog.isAuthor(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 글만 수정/삭제할 수 있습니다.");
        }
        return worklog;
    }
}

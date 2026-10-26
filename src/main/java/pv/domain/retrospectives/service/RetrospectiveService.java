package pv.domain.retrospectives.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pv.domain.board.entity.Board;
import pv.domain.board.repository.BoardRepository;
import pv.domain.member.entity.Member;
import pv.domain.member.repository.MemberRepository;
import pv.domain.retrospectives.dto.request.RetrospectiveUpdateRequest;
import pv.domain.retrospectives.dto.response.RetrospectiveResponse;
import pv.domain.retrospectives.entity.Retrospective;
import pv.domain.retrospectives.repository.RetrospectiveRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RetrospectiveService {
    private final RetrospectiveRepository retrospectiveRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;

    public List<RetrospectiveResponse> getByBoard(Long memberId, Long boardId) {
        Member member = validateApproved(memberId);
        Board board = findBoard(boardId);
        validateTeamMember(member, board);
        if (!board.isClosed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료된 보드의 회고만 조회할 수 있습니다.");
        }

        List<Retrospective> retrospectives = retrospectiveRepository.findAllByBoardIdOrderByIdAsc(boardId);
        Map<Long, String> names = memberNames(retrospectives.stream()
                .map(Retrospective::getMemberId)
                .collect(Collectors.toSet()));

        return retrospectives.stream()
                .map(r -> RetrospectiveResponse.from(r, names.get(r.getMemberId())))
                .toList();
    }

    @Transactional
    public void updateMine(Long memberId, Long boardId, RetrospectiveUpdateRequest request) {
        Member member = validateApproved(memberId);
        Board board = findBoard(boardId);
        validateTeamMember(member, board);
        Retrospective retrospective = retrospectiveRepository.findByBoardIdAndMemberId(boardId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회고를 찾을 수 없습니다."));
        retrospective.update(request.content());

        if (!board.isClosed()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "종료된 보드의 회고만 수정할 수 있습니다."
            );
        }
    }

    private Board findBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "보드를 찾을 수 없습니다."));
    }


    private void validateTeamMember(Member member, Board board) {
        if (!Objects.equals(member.getTeamId(), board.getTeamId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "보드를 찾을 수 없습니다."
            );
        }
    }

    private Member validateApproved(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 회원입니다."));
        if (!member.isJoined()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "가입 승인되지 않은 회원입니다.");
        }
        return member;
    }

    // 이름이 null인 멤버가 있어도 NPE가 나지 않도록 직접 담는다
    private Map<Long, String> memberNames(Set<Long> memberIds) {
        Map<Long, String> names = new HashMap<>();
        if (memberIds.isEmpty()) return names;
        memberRepository.findAllById(memberIds).forEach(m -> names.put(m.getId(), m.getName()));
        return names;
    }
}
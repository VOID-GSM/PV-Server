package pv.domain.comments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.comments.dto.CommentDto;
import pv.domain.comments.dto.FeedbackRequestDto;
import pv.domain.comments.entity.Anchor;
import pv.domain.comments.entity.Comment;
import pv.domain.comments.entity.FeedbackRequest;
import pv.domain.comments.entity.TargetType;
import pv.domain.comments.repository.CommentRepository;
import pv.domain.comments.repository.FeedbackRequestRepository;
import pv.domain.member.entity.Member;
import pv.global.exception.CustomException;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.repository.MemberRepository;

import java.util.List;
import java.util.HashSet;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final FeedbackRequestRepository feedbackRequestRepository;
    private final MemberRepository memberRepository;

    // 댓글 부분
    @Transactional(readOnly = true)
    public List<CommentDto.Response> getComments(Long memberId, TargetType type, Long targetId) {
        Member me = getJoinedMember(memberId);
        return commentRepository.findByTeamIdAndTargetTypeAndTargetIdOrderByCreatedAtAsc(me.getTeamId(), type, targetId)
                .stream().map(CommentDto.Response::from).toList();
    }

    public CommentDto.Response create(Long memberId, CommentDto.Create req) {
        Member me = getJoinedMember(memberId);
        Anchor anchor = toAnchor(req.targetType(), req.targetId(), req.anchor());
        Comment saved = commentRepository.save(
                Comment.create(me.getTeamId(), req.targetType(), req.targetId(), memberId, req.content(), anchor));
        return CommentDto.Response.from(saved);
    }

    public CommentDto.Response update(Long memberId, Long commentId, CommentDto.Update req) {
        getJoinedMember(memberId);
        Comment comment = getComment(commentId);
        if (!comment.isAuthor(memberId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "본인 댓글만 수정할 수 있습니다.");
        }
        comment.edit(req.content());
        // @UpdateTimestamp 는 flush 시점에 갱신되므로, 응답의 updatedAt 반영을 위해 명시적 flush함
        commentRepository.flush();
        return CommentDto.Response.from(comment);
    }

    // Member 머지 후 관리자 삭제 허용 재구현 할 예정
    public void delete(Long memberId, Long commentId) {
        Member me = getJoinedMember(memberId);
        Comment comment = getComment(commentId);
        boolean leaderOfTeam = me.isLeader() && me.getTeamId().equals(comment.getTeamId());
             if (!comment.isAuthor(memberId) && !leaderOfTeam) {
                 throw new CustomException(HttpStatus.FORBIDDEN, "댓글을 삭제할 권한이 없습니다.");
             }
        commentRepository.delete(comment);
    }

    public List<FeedbackRequestDto.Response> requestFeedback(Long memberId, FeedbackRequestDto.Create req) {
        Member me = getJoinedMember(memberId);
        Long teamId = me.getTeamId();
        int myGen = me.getGeneration().getNumber();

        List<Long> receivers = switch (req.scope()) {
            case MEMBERS -> req.memberIds() == null || req.memberIds().isEmpty()
                    ? List.of()
                    : memberRepository.findJoinedIds(teamId, req.memberIds());
            case SENIOR_ALL -> memberRepository.findSeniorIds(teamId, myGen);
            case SENIOR_SAME_MAJOR -> me.getMajors().isEmpty()
                    ? List.of()
                    : memberRepository.findSeniorIdsByMajors(teamId, myGen, me.getMajors());
        };

        var alreadyPending = new HashSet<>(
                feedbackRequestRepository.findPendingReceiverIds(req.targetType(), req.targetId()));

        List<FeedbackRequest> requests = receivers.stream()
                .distinct()
                .filter(id -> !id.equals(memberId) && !alreadyPending.contains(id))
                .map(id -> FeedbackRequest.create(req.targetType(), req.targetId(), memberId, id, req.message()))
                .toList();

        if (requests.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "요청을 보낼 대상이 없습니다.");
        }
        return feedbackRequestRepository.saveAll(requests).stream()
                .map(FeedbackRequestDto.Response::from).toList();
    }

    // 피드백 요청 부분
    @Transactional(readOnly = true)
    public List<FeedbackRequestDto.Response> getMyRequests(Long memberId) {
        return feedbackRequestRepository.findByReceiverIdAndCompletedAtIsNullOrderByCreatedAtDesc(memberId)
                .stream().map(FeedbackRequestDto.Response::from).toList();
    }

    public void complete(Long memberId, Long requestId) {
        FeedbackRequest request = feedbackRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "피드백 요청을 찾을 수 없습니다."));
        if (!request.isReceiver(memberId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "요청받은 사람만 완료할 수 있습니다.");
        }
        request.complete();
    }

    // 공통 부분
    private Anchor toAnchor(TargetType type, Long targetId, CommentDto.AnchorRequest req) {
        if (req == null) return null;
        String blockText = getBlockText(type, targetId, req.blockIndex());
        return Anchor.of(req.blockIndex(), blockText, req.startOffset(), req.endOffset());
    }

    private String getBlockText(TargetType type, Long targetId, int blockIndex) {
        throw new CustomException(HttpStatus.NOT_IMPLEMENTED, "인라인 댓글은 아직 지원하지 않습니다.");
    }

    private Comment getComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
    }

    private Member getJoinedMember(Long id) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "회원을 찾을 수 없습니다."));
        if (m.getStatus() != MemberStatus.JOINED) {
            throw new CustomException(HttpStatus.FORBIDDEN, "가입 승인 후 이용할 수 있습니다.");
        }
        return m;
    }
}
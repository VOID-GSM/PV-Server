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
import pv.global.exception.CustomException;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final FeedbackRequestRepository feedbackRequestRepository;

    // 댓글 부분
    @Transactional(readOnly = true)
    public List<CommentDto.Response> getComments(TargetType type, Long targetId) {
        return commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(type, targetId)
                .stream().map(CommentDto.Response::from).toList();
    }

    public CommentDto.Response create(Long memberId, CommentDto.Create req) {
        Anchor anchor = toAnchor(req.targetType(), req.targetId(), req.anchor());
        Comment saved = commentRepository.save(
                Comment.create(req.targetType(), req.targetId(), memberId, req.content(), anchor));
        return CommentDto.Response.from(saved);
    }

    public CommentDto.Response update(Long memberId, Long commentId, CommentDto.Update req) {
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
        Comment comment = getComment(commentId);
        if (!comment.isAuthor(memberId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        commentRepository.delete(comment);
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
}
package pv.domain.posts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.activity.entity.ActivitySession;
import pv.domain.activity.repository.ActivityMemberRepository;
import pv.domain.activity.repository.ActivitySessionRepository;
import pv.domain.interviews.repository.InterviewQuestionRepository;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.repository.MemberRepository;
import pv.global.exception.CustomException;
import pv.domain.posts.dto.PostCreateRequest;
import pv.domain.posts.dto.PostResponse;
import pv.domain.posts.dto.PostSearchCondition;
import pv.domain.posts.dto.PostSummaryResponse;
import pv.domain.posts.dto.PostUpdateRequest;
import pv.domain.posts.entity.Post;
import pv.domain.posts.repository.PostRepository;
import org.springframework.data.domain.Sort;

import java.util.Set;
import static pv.domain.posts.repository.PostSpecs.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ActivitySessionRepository activitySessionRepository;
    private final ActivityMemberRepository activityMemberRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdAt", "id");

    public Page<PostSummaryResponse> getPosts(PostSearchCondition cond, Long currentMemberId, Pageable pageable) {
        validateSort(pageable.getSort());
        findJoinedMember(currentMemberId);
        if (cond.from() != null && cond.to() != null && cond.from().isAfter(cond.to())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "from이 to보다 늦습니다.");
        }
        Specification<Post> spec = Specification.where(activityId(cond.activityId()))
                .and(sessionId(cond.sessionId()))
                .and(authorId(resolveAuthor(cond.author(), currentMemberId)))
                .and(createdFrom(cond.from()))
                .and(createdTo(cond.to()));
        return postRepository.findAll(spec, pageable).map(PostSummaryResponse::from);
    }

    @Transactional
    public PostResponse create(PostCreateRequest req, Long currentMemberId) {
        Member member = findJoinedMember(currentMemberId);
        Long activityId = null;
        if (req.sessionId() != null) {
            ActivitySession session = activitySessionRepository.findById(req.sessionId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회차가 없습니다."));
            activityId = session.getActivityId();
            validateParticipant(activityId, member);
        }
        if (req.questionId() != null && !interviewQuestionRepository.existsById(req.questionId())) {
            throw new CustomException(HttpStatus.NOT_FOUND, "면접 질문이 없습니다.");
        }
        Post post = Post.create(
                member.getId(), 
                activityId,
                req.sessionId(),
                req.questionId(),
                req.title(),
                req.content()
        );
        return PostResponse.from(postRepository.save(post));
    }

    public PostResponse getPost(Long postId, Long currentMemberId) {
        findJoinedMember(currentMemberId);
        return PostResponse.from(findPost(postId));
    }

    @Transactional
    public PostResponse update(Long postId, PostUpdateRequest req, Long currentMemberId) {
        findJoinedMember(currentMemberId);
        Post post = findPost(postId);
        if (!post.isWrittenBy(currentMemberId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "본인 글만 수정할 수 있습니다.");
        }
        post.update(req.title(), req.content());
        postRepository.flush();
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long postId, Long currentMemberId) {
        Member member = findJoinedMember(currentMemberId);
        Post post = findPost(postId);
        if (!post.isWrittenBy(currentMemberId) && !member.isLeader()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        postRepository.delete(post);
    }

    public List<PostResponse> compare(Long sessionId, Long questionId, Long currentMemberId) {
        findJoinedMember(currentMemberId);
        List<Post> posts = sessionId != null
                ? postRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId)
                : postRepository.findAllByQuestionIdOrderByCreatedAtAsc(questionId);
        return posts.stream().map(PostResponse::from).toList();
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "정리글이 없습니다."));
    }
    
    private void validateParticipant(Long activityId, Member member) {
        if (member.isLeader()) {
            return;
        }
        if (activityMemberRepository.findByActivityIdAndMemberId(activityId, member.getId()).isEmpty()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "활동 참여자만 정리글을 작성할 수 있습니다.");
        }
    }

    private Member findJoinedMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회원이 없습니다."));
        if (member.getStatus() != MemberStatus.JOINED) {
            throw new CustomException(HttpStatus.FORBIDDEN, "가입 승인 후 이용할 수 있습니다.");
        }
        return member;
    }

    private Long resolveAuthor(String author, Long currentMemberId) {
        if (author == null || author.isBlank()) {
            return null;
        }
        if ("me".equalsIgnoreCase(author)) {
            return currentMemberId;
        }
        try {
            return Long.parseLong(author);
        } catch (NumberFormatException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "author는 'me' 또는 숫자 id 여야합니다.");
        }

    }

    private void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "정렬은 createdAt, id만 가능합니다.");
            }
        }
    }
}

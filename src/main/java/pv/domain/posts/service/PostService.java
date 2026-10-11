package pv.domain.posts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.global.exception.CustomException;
import pv.domain.posts.dto.PostCreateRequest;
import pv.domain.posts.dto.PostResponse;
import pv.domain.posts.dto.PostSearchCondition;
import pv.domain.posts.dto.PostSummaryResponse;
import pv.domain.posts.dto.PostUpdateRequest;
import pv.domain.posts.entity.Post;
import pv.domain.posts.repository.PostRepository;

import static pv.domain.posts.repository.PostSpecs.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    public Page<PostSummaryResponse> getPosts(PostSearchCondition cond, Long currentMemberId, Pageable pageable) {
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
        Post post = Post.create(
                currentMemberId,
                req.activityId(),
                req.sessionId(),
                req.questionId(),
                req.title(),
                req.content()
        );
        return PostResponse.from(postRepository.save(post));
    }

    public PostResponse getPost(Long postId) {
        return PostResponse.from(findPost(postId));
    }

    @Transactional
    public PostResponse update(Long postId, PostUpdateRequest req, Long currentMemberId) {
        Post post = findPost(postId);
        if (!post.isWrittenBy(currentMemberId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "본인 글만 수정할 수 있습니다.");
        }
        post.update(req.title(), req.content());
        postRepository.flush();
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long postId, Long currentMemberId, boolean isLeader) {
        Post post = findPost(postId);
        if (!post.isWrittenBy(currentMemberId) && !isLeader) {
            throw new CustomException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        postRepository.delete(post);
    }

    public List<PostResponse> compare(Long sessionId, Long questionId) {
        if ((sessionId == null) == (questionId == null)) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "sessionId와 questionId 중 정확히 하나만 지정하세요.");
        }
        List<Post> posts = sessionId != null
                ? postRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId)
                : postRepository.findAllByQuestionIdOrderByCreatedAtAsc(questionId);
        return posts.stream().map(PostResponse::from).toList();
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "정리글이 없습니다."));
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
}

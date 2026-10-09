package pv.domain.posts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.posts.dto.PostCreateRequest;
import pv.domain.posts.dto.PostResponse;
import pv.domain.posts.dto.PostSearchCondition;
import pv.domain.posts.dto.PostSummaryResponse;
import pv.domain.posts.dto.PostUpdateRequest;
import pv.domain.posts.service.PostService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<PostSummaryResponse>> getPosts(
            @Valid @ModelAttribute PostSearchCondition cond,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable,
            @RequestAttribute("memberId") Long memberId
            ) {
        return ResponseEntity.ok(postService.getPosts(cond, memberId, pageable));
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(
            @Valid @RequestBody PostCreateRequest req,
            @RequestAttribute("memberId") Long memberId
    ) {
        PostResponse res = postService.create(req, memberId);
        return ResponseEntity.created(URI.create("/posts/" + res.id())).body(res);
    }

    @GetMapping("/compare")
    public ResponseEntity<List<PostResponse>> compare(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long questionId
    ) {
        return ResponseEntity.ok(postService.compare(sessionId, questionId));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<PostResponse> update(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest req,
            @RequestAttribute("memberId") Long memberId
    ) {
        return ResponseEntity.ok(postService.update(postId, req, memberId));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @RequestAttribute("memberId") Long memberId,
            @RequestAttribute(value = "isLeader", required = false) Boolean isLeader
    ) {
        postService.delete(postId, memberId, Boolean.TRUE.equals(isLeader));
        return ResponseEntity.noContent().build();
    }
}

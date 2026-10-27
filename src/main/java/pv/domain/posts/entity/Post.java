package pv.domain.posts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import java.util.Objects;


import java.time.LocalDateTime;

@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_posts_author", columnList = "author_id"),
                @Index(name = "idx_posts_activity", columnList = "activity_id"),
                @Index(name = "idx_posts_session_created", columnList = "session_id, created_at"),
                @Index(name = "idx_posts_question_created", columnList = "question_id, created_at"),
                @Index(name = "idx_posts_created_at", columnList = "created_at"),
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Post(Long authorId, Long activityId, Long sessionId,
                 Long questionId, String title, String content, LocalDateTime createdAt) {
        this.authorId = authorId;
        this.activityId = activityId;
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.title = title;
        this.content = content;
        this.createdAt = Objects.requireNonNull(createdAt, "작성일은 필수입니다.");
    }

    public static Post create(Long authorId, Long activityId, Long sessionId,
                              Long questionId, String title, String content, LocalDateTime createdAt) {
        return new Post(authorId, activityId, sessionId, questionId, title, content, createdAt);
    }

    public static Post create(Long authorId, Long activityId, Long sessionId,
                              Long questionId, String title, String content) {
        return new Post(authorId, activityId, sessionId, questionId, title, content, null);
    }

    // 노션 가져오기용: 작성일을 직접 지정
    public static Post createImported(Long authorId, Long activityId,
                                      String title, String content, LocalDateTime createdAt) {
        return new Post(authorId, activityId, null, null, title, content, createdAt);
    }

    public void update(String title, String content) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
    }

    public boolean isWrittenBy(Long memberId) {
        return this.authorId.equals(memberId);
    }
}
package pv.domain.comments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.http.HttpStatus;
import pv.global.exception.CustomException;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments", indexes = @Index(name = "idx_comments_target", columnList = "target_type, target_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Embedded
    private Anchor anchor;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "댓글 내용이 비었습니다.");
        }
    }
}

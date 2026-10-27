package pv.domain.worklogs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import pv.domain.board.entity.Board;
import pv.domain.member.entity.Member;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Worklog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private Member author;

    @ManyToOne(fetch = FetchType.LAZY)
    private Board board;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Visibility visibility;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    private Worklog(Member author, Board board, String title, String content,
                    Visibility visibility, LocalDateTime createdAt) {
        this.author = Objects.requireNonNull(author, "작성자는 필수입니다.");
        this.board = board;
        this.title = Objects.requireNonNull(title, "제목은 필수입니다.");
        this.content = Objects.requireNonNull(content, "내용은 필수입니다.");
        this.visibility = (visibility != null) ? visibility : Visibility.PRIVATE;
        this.createdAt = Objects.requireNonNull(createdAt, "작성일은 필수입니다.");
    }

    public void update(String title, String content, Visibility visibility) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (visibility != null) this.visibility = visibility;
    }

    public boolean isAuthor(Long memberId) {
        return author.getId().equals(memberId);
    }

    public boolean isVisibleTo(Long memberId) {
        return visibility == Visibility.PUBLIC || isAuthor(memberId);
    }
}
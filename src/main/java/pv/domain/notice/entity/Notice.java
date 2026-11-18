package pv.domain.notice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pv.domain.member.entity.Major;
import pv.domain.notice.entity.NoticeScope;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teamId;

    @Column(nullable = false)
    private Long authorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeScope scope;

    // scope에 따라 하나만 채워진다.
    private Long boardId;
    private Long activityId;

    // TARGET 범위: 기수 AND 전공(여러 개 중 하나라도 보유)
    @ElementCollection
    @CollectionTable(name = "notice_target_generations", joinColumns = @JoinColumn(name = "notice_id"))
    @Column(name = "generation_id")
    private Set<Long> targetGenerationIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "notice_target_majors", joinColumns = @JoinColumn(name = "notice_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "major")
    private Set<Major> targetMajors = new HashSet<>();

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 마지막 재알림 시각 (재알림 간격 제한용)
    private LocalDateTime lastRemindedAt;

    public Notice(Long teamId, Long authorId, NoticeScope scope, Long boardId, Long activityId,
                  Set<Long> generationIds, Set<Major> majors,
                  String title, String content, LocalDateTime now) {
        this.teamId = teamId;
        this.authorId = authorId;
        this.scope = scope;
        this.boardId = boardId;
        this.activityId = activityId;
        if (generationIds != null) this.targetGenerationIds.addAll(generationIds);
        if (majors != null) this.targetMajors.addAll(majors);
        this.title = title;
        this.content = content;
        this.createdAt = now;
    }

    // null이 아닌 값만 수정. 범위(대상자)는 바꿀 수 없다.
    public void update(String title, String content, LocalDateTime now) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        this.updatedAt = now;
    }

    public boolean isAuthor(Long memberId) {
        return authorId.equals(memberId);
    }

    // 아직 재알림을 보낼 수 없으면 남은 시간을, 보낼 수 있으면 Duration.ZERO를 반환
    public Duration remainingRemindCooldown(LocalDateTime now, Duration interval) {
        if (lastRemindedAt == null) {
            return Duration.ZERO;
        }
        Duration remaining = interval.minus(Duration.between(lastRemindedAt, now));
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public void markReminded(LocalDateTime now) {
        this.lastRemindedAt = now;
    }
}

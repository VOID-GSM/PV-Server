package pv.domain.calendar.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.http.HttpStatus;
import pv.global.exception.CustomException;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "calendar_event",
        indexes = {@Index(name = "idx_calendar_event_owner_starts", columnList = "owner_id, starts_at"),
                @Index(name = "idx_calendar_event_board_starts", columnList = "board_id, starts_at"),
                @Index(name = "idx_calendar_event_activity_starts", columnList = "activity_id, starts_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "text")
    private String memo;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(nullable = false)
    private boolean allDay;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "board_id")
    private Long boardId;

    @Column(name = "activity_id")
    private Long activityId;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ElementCollection
    @CollectionTable(name = "calendar_event_assignees",
            joinColumns = @JoinColumn(name = "event_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "member_id"}),
            indexes = @Index(name = "idx_calendar_event_assignees_member", columnList = "member_id"))
    @Column(name = "member_id", nullable = false)
    @BatchSize(size = 100)
    private Set<Long> assigneeIds = new LinkedHashSet<>();

    public static CalendarEvent meeting(Long boardId, Long creatorId, String title, String memo,
                                        Instant startsAt, Instant endsAt, boolean allDay,
                                        Collection<Long> assigneeIds) {
        CalendarEvent e = personal(creatorId, title, memo, startsAt, endsAt, allDay);
        e.type = EventType.MEETING;
        e.boardId = Objects.requireNonNull(boardId, "보드는 필수입니다.");
        e.changeAssignees(assigneeIds);
        return e;
    }

    public void changeAssignees(Collection<Long> assigneeIds) {
        if (assigneeIds == null) return;
        this.assigneeIds.clear();
        this.assigneeIds.addAll(assigneeIds);
    }

    public boolean isMeeting() {
        return type == EventType.MEETING;
    }

    public static CalendarEvent personal(Long ownerId, String title, String memo,
                                         Instant startsAt, Instant endsAt, boolean allDay) {
        Objects.requireNonNull(startsAt, "시작 시각은 필수입니다.");
        Objects.requireNonNull(endsAt, "종료 시각은 필수입니다.");
        validateRange(startsAt, endsAt);

        CalendarEvent e = new CalendarEvent();
        e.type = EventType.PERSONAL;
        e.ownerId = Objects.requireNonNull(ownerId, "작성자는 필수입니다.");
        e.title = Objects.requireNonNull(title, "제목은 필수입니다.");
        e.memo = (memo == null || memo.isBlank() ? null : memo);
        e.startsAt = startsAt;
        e.endsAt = endsAt;
        e.allDay = allDay;
        return e;
    }

    public void update(String title, String memo, Instant startsAt, Instant endsAt, Boolean allDay) {
        Instant s = startsAt != null ? startsAt : this.startsAt;
        Instant en = endsAt != null ? endsAt : this.endsAt;
        validateRange(s, en);

        if (title != null) this.title = title;
        if (memo != null) this.memo = memo.isBlank() ? null : memo;
        if (allDay != null) this.allDay = allDay;
        this.startsAt = s;
        this.endsAt = en;
    }

    public boolean isPersonalOf(Long memberId) {
        return type == EventType.PERSONAL && ownerId.equals(memberId);
    }

    private static void validateRange(Instant startsAt, Instant endsAt) {
        if (endsAt.isBefore(startsAt)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "종료 시각이 시작 시각보다 빠릅니다.");
        }
    }

    public void changeCompletion(boolean completed, Instant now) {
        if (completed) {
            if (this.completedAt == null) {
                this.completedAt = now;
            }
        } else {
            this.completedAt = null;
        }
    }

    public boolean isCompleted() {
        return completedAt != null;
    }
}

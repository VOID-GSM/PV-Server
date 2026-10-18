package pv.domain.calendar.entity;

import jakarta.persistence.Column;
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

import java.time.Instant;
import java.util.Objects;

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
}

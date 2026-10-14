package pv.domain.activity.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "session_no"}))
public class ActivitySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long activityId;

    @Column(nullable = false)
    private int sessionNo;

    @Column(nullable = false)
    private LocalDate sessionDate;

    private LocalTime sessionTime;

    @Column(length = 200)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ActivitySession(Long activityId, int sessionNo, LocalDate sessionDate, LocalTime sessionTime) {
        this.activityId = activityId;
        this.sessionNo = sessionNo;
        this.sessionDate = sessionDate;
        this.sessionTime = sessionTime;
        this.status = SessionStatus.SCHEDULED;
    }

    // null이 아닌 값만 수정
    public void update(String topic, LocalDate sessionDate, LocalTime sessionTime, SessionStatus status) {
        if (topic != null) this.topic = topic;
        if (sessionDate != null) this.sessionDate = sessionDate;
        if (sessionTime != null) this.sessionTime = sessionTime;
        if (status != null) this.status = status;
    }

    public void confirmTopic(String topic) { this.topic = topic; }
}
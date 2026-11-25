package pv.domain.board.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "deadline_alert_log", uniqueConstraints = @UniqueConstraint(
    name = "uk_deadline_alert_log", columnNames = {"deadline_id", "deadline_date", "days_left"}))
public class DeadlineAlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deadline_id", nullable = false)
    private Long deadlineId;

    @Column(name = "deadline_date", nullable = false)
    private LocalDate deadlineDate;

    @Column(name = "days_left", nullable = false)
    private int daysLeft;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public DeadlineAlertLog(Long deadlineId, LocalDate deadlineDate, int daysLeft, LocalDateTime sentAt) {
        this.deadlineId = deadlineId;
        this.deadlineDate = deadlineDate;
        this.daysLeft = daysLeft;
        this.sentAt = sentAt;
    }
}

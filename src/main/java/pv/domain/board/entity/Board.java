package pv.domain.board.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardType type;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @ElementCollection
    @CollectionTable(name = "board_alert_days", joinColumns = @JoinColumn(name = "board_id"))
    @Column(name = "alert_day")
    @BatchSize(size = 100)
    private Set<Integer> alertDays = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Board(Long teamId, String name, BoardType type, String color,
                 LocalDate startDate, LocalDate endDate, Set<Integer> alertDays) {
        this.teamId = teamId;
        this.name = name;
        this.type = type;
        this.color = color;
        this.startDate = startDate;
        this.endDate = endDate;
        if (alertDays != null) this.alertDays.addAll(alertDays);
        this.status = BoardStatus.ACTIVE;
    }

    public boolean isClosed() {
        return status == BoardStatus.CLOSED;
    }

    // null이 아닌 값만 수정
    public void update(String name, String color, LocalDate startDate, LocalDate endDate, Set<Integer> alertDays) {
        if (name != null) this.name = name;
        if (color != null) this.color = color;
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
        if (alertDays != null) {
            this.alertDays.clear();
            this.alertDays.addAll(alertDays);
        }
    }

    public void close() {
        this.status = BoardStatus.CLOSED;
    }
}
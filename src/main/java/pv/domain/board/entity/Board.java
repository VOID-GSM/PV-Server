package pv.domain.board.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    // D-day 알림 단계 (예: 7, 3, 1 -> D-7, D-3, D-1에 알림)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "board_alert_days", joinColumns = @JoinColumn(name = "board_id"))
    @Column(name = "alert_day")
    private Set<Integer> alertDays = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardStatus status;

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
        this.createdAt = LocalDateTime.now();
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
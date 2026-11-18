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
public class Deadline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long boardId;

    private String name;

    private LocalDate deadlineDate;

    // 담당자 (Member ID, 없을 수 있음)
    private Long assigneeId;

    // 마일스톤으로 확정된 마감인지
    private boolean milestone;

    private LocalDateTime createdAt;

    public Deadline(Long boardId, String name, LocalDate deadlineDate, Long assigneeId, boolean milestone) {
        this.boardId = boardId;
        this.name = name;
        this.deadlineDate = deadlineDate;
        this.assigneeId = assigneeId;
        this.milestone = milestone;
        this.createdAt = LocalDateTime.now();
    }

    // null이 아닌 값만 수정
    public void update(String name, LocalDate deadlineDate, Long assigneeId) {
        if (name != null) this.name = name;
        if (deadlineDate != null) this.deadlineDate = deadlineDate;
        if (assigneeId != null) this.assigneeId = assigneeId;
    }
}

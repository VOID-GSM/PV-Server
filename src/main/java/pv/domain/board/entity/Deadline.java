package pv.domain.board.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @ElementCollection
    @CollectionTable(name = "deadline_assignees",
            joinColumns = @JoinColumn(name = "deadline_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"deadline_id", "member_id"}),
            indexes = @Index(name = "idx_deadline_assignees_member", columnList = "member_id"))
    @Column(name = "member_id", nullable = false)
    @BatchSize(size = 100)
    private Set<Long> assigneeIds = new LinkedHashSet<>();

    private boolean milestone;

    @Column(columnDefinition = "text")
    private String description;

    private LocalDateTime createdAt;

    public Deadline(Long boardId, String name, LocalDate deadlineDate, Collection<Long> assigneeIds,
                    String description, boolean milestone) {
        this.boardId = boardId;
        this.name = name;
        this.deadlineDate = deadlineDate;
        if (assigneeIds != null) this.assigneeIds.addAll(assigneeIds);
        this.description = blankToNull(description);
        this.milestone = milestone;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, LocalDate deadlineDate, Collection<Long> assigneeIds, String description) {
        if (name != null) this.name = name;
        if (deadlineDate != null) this.deadlineDate = deadlineDate;
        if (assigneeIds != null) {
            this.assigneeIds.clear();
            this.assigneeIds.addAll(assigneeIds);
        }
        if (description != null) this.description = blankToNull(description);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
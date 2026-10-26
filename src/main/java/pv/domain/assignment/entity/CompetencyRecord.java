package pv.domain.assignment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 통과 처리 시 남기는 역량 기록 (단순 기록)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "competency_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_competency_submission_name",
                columnNames = {"submission_id", "competency"}),
        indexes = {
                @Index(name = "idx_competency_member", columnList = "member_id"),
                @Index(name = "idx_competency_assignment", columnList = "assignment_id")
        })
public class CompetencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(nullable = false, length = 100)
    private String competency;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    public CompetencyRecord(Long teamId, Long memberId, Long assignmentId, Long submissionId,
                            String competency, LocalDateTime recordedAt) {
        this.teamId = teamId;
        this.memberId = memberId;
        this.assignmentId = assignmentId;
        this.submissionId = submissionId;
        this.competency = competency;
        this.recordedAt = recordedAt;
    }
}

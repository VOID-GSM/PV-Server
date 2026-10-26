package pv.domain.assignment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pv.domain.member.entity.Member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "submissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_submission_assignment_submitter",
                columnNames = {"assignment_id", "submitter_id"}),
        indexes = @Index(name = "idx_submissions_submitter", columnList = "submitter_id"))
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 낙관적 락: 비관적 락을 우회한 갱신까지 방어
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitter_id")
    private Member submitter;

    @Column(columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> fileKeys = new ArrayList<>();

    @Column(length = 500)
    private String githubUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status;

    // 최초 제출이 마감 이후였으면 true (재제출해도 유지)
    @Column(nullable = false)
    private boolean late;

    @Column(columnDefinition = "text")
    private String feedback;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false)
    private LocalDateTime lastSubmittedAt;

    private LocalDateTime reviewedAt;

    public Submission(Assignment assignment, Member submitter, String content, List<String> fileKeys,
                      String githubUrl, boolean late, LocalDateTime now) {
        this.assignment = assignment;
        this.submitter = submitter;
        this.content = content;
        this.fileKeys = new ArrayList<>(fileKeys);
        this.githubUrl = githubUrl;
        this.status = SubmissionStatus.SUBMITTED;
        this.late = late;
        this.submittedAt = now;
        this.lastSubmittedAt = now;
    }

    public boolean isSubmittedBy(Long memberId) {
        return submitter.getId().equals(memberId);
    }

    public boolean belongsTo(Long assignmentId) {
        return assignment.getId().equals(assignmentId);
    }

    public void resubmit(String content, List<String> fileKeys, String githubUrl, LocalDateTime now) {
        this.content = content;
        this.fileKeys = new ArrayList<>(fileKeys);
        this.githubUrl = githubUrl;
        this.status = SubmissionStatus.SUBMITTED;
        this.lastSubmittedAt = now;
    }

    public void pass(String feedback, LocalDateTime now) {
        this.status = SubmissionStatus.PASSED;
        this.feedback = feedback;
        this.reviewedAt = now;
    }

    public void requestResubmission(String comment, LocalDateTime now) {
        this.status = SubmissionStatus.RESUBMIT_REQUESTED;
        this.feedback = comment;
        this.reviewedAt = now;
    }
}

package pv.domain.assignment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "assignment_recipients",
        uniqueConstraints = @UniqueConstraint(name = "uk_assignment_recipient",
                columnNames = {"assignment_id", "member_id"}),
        indexes = @Index(name = "idx_assignment_recipients_member", columnList = "member_id"))
public class AssignmentRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;
}
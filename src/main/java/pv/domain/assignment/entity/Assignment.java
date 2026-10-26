package pv.domain.assignment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "assignments", indexes = {
        @Index(name = "idx_assignments_team", columnList = "team_id"),
        @Index(name = "idx_assignments_author", columnList = "author_id")
})
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private Member author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentTargetType targetType;

    @ElementCollection
    @CollectionTable(name = "assignment_target_majors", joinColumns = @JoinColumn(name = "assignment_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "major")
    private Set<Major> targetMajors = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "assignment_target_members", joinColumns = @JoinColumn(name = "assignment_id"))
    @Column(name = "member_id")
    private Set<Long> targetMemberIds = new HashSet<>();

    // S3 object key 목록
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> fileKeys = new ArrayList<>();

    // 통과 시 실적으로 집계될 스킬 ID (skills 테이블은 삭제 없이 비활성화만 하므로 FK 없이 ID만 보관)
    @ElementCollection
    @CollectionTable(name = "assignment_skills",
            joinColumns = @JoinColumn(name = "assignment_id"),
            indexes = @Index(name = "idx_assignment_skills_skill", columnList = "skill_id"))
    @Column(name = "skill_id", nullable = false)
    private Set<Long> skillIds = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Assignment(Long teamId, Member author, String title, String description, LocalDateTime deadline,
                      AssignmentTargetType targetType, Set<Major> majors, Set<Long> memberIds,
                      List<String> fileKeys, Set<Long> skillIds, LocalDateTime createdAt) {
        this.teamId = teamId;
        this.author = author;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.targetType = targetType;
        this.targetMajors.addAll(majors);
        this.targetMemberIds.addAll(memberIds);
        this.fileKeys = new ArrayList<>(fileKeys);
        this.skillIds.addAll(skillIds);
        this.createdAt = createdAt;
    }
    public boolean isAuthor(Long memberId) {
        return author.getId().equals(memberId);
    }

    // 출제자 본인은 대상자에서 제외
    public boolean isTarget(Member member) {
        if (!teamId.equals(member.getTeamId()) || isAuthor(member.getId())) {
            return false;
        }
        return switch (targetType) {
            case ALL -> true;
            case MAJOR -> member.getMajors().stream().anyMatch(targetMajors::contains);
            case MEMBER -> targetMemberIds.contains(member.getId());
        };
    }

    public boolean isPastDeadline(LocalDateTime now) {
        return now.isAfter(deadline);
    }

    // null이 아닌 값만 수정
    public void update(String title, String description, LocalDateTime deadline,
                       List<String> fileKeys, Set<Long> skillIds) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (deadline != null) this.deadline = deadline;
        if (fileKeys != null) this.fileKeys = new ArrayList<>(fileKeys);
        if (skillIds != null) {
            this.skillIds.clear();
            this.skillIds.addAll(skillIds);
        }
    }
}
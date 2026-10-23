package pv.domain.skills.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import pv.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_skills",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_skills_member_skill",
        columnNames = {"member_id", "skill_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSkill {
    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private int score;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime assessedAt;

    public MemberSkill(Member member, Skill skill, int score) {
        validateScore(score);
        this.member = member;
        this.skill = skill;
        this.score = score;
    }

    private final void validateScore(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException("score must be between 1 and 5: " + score);
        }
    }
}

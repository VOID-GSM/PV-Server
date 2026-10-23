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
import org.springframework.http.HttpStatus;
import pv.domain.member.entity.Member;
import pv.global.exception.CustomException;

import java.time.LocalDateTime;
import java.util.Objects;

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
        this.member = Objects.requireNonNull(member, "회원은 필수입니다.");
        this.skill = Objects.requireNonNull(skill, "스킬은 필수입니다.");
        this.score = score;
    }

    public void reassess(int score) {
        validateScore(score);
        this.score = score;
    }

    private static void validateScore(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "점수는 " + MIN_SCORE + "~" + MAX_SCORE + " 사이여야 합니다: " + score);
        }
    }
}

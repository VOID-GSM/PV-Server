package pv.domain.interviews.entity;

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
import pv.domain.member.entity.Member;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "interview_answer",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_interview_answer_question_member",
                columnNames = {"question_id", "member_id"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public InterviewAnswer(InterviewQuestion question, Member member, String content) {
        this(question, member, content, null);
    }

    // 노션 가져오기용: 작성일을 직접 지정
    public InterviewAnswer(InterviewQuestion question, Member member, String content, LocalDateTime createdAt) {
        this.question = Objects.requireNonNull(question, "질문은 필수입니다.");
        this.member = Objects.requireNonNull(member, "회원은 필수입니다.");
        this.content = Objects.requireNonNull(content, "답변 내용은 필수입니다.");
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public void updateContent(String content) {
        this.content = Objects.requireNonNull(content, "답변 내용은 필수입니다.");
    }

    public boolean isWrittenBy(Long memberId) {
        return this.member.getId().equals(memberId);
    }

    public boolean belongsTo(Long questionId) {
        return this.question.getId().equals(questionId);
    }
}
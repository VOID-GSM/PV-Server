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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pv.global.entity.ContentSource;
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

    // 기존 데이터가 있어도 서버가 뜨도록 DB 기본값을 MANUAL로 지정
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, columnDefinition = "varchar(10) default 'MANUAL'")
    private ContentSource source;
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public InterviewAnswer(InterviewQuestion question, Member member, String content, LocalDateTime createdAt) {
        this(question, member, content, createdAt, ContentSource.MANUAL);
    }

    // 노션 가져오기 등 출처를 직접 지정할 때
    public InterviewAnswer(InterviewQuestion question, Member member, String content,
                           LocalDateTime createdAt, ContentSource source) {
        this.question = Objects.requireNonNull(question, "질문은 필수입니다.");
        this.member = Objects.requireNonNull(member, "회원은 필수입니다.");
        this.content = Objects.requireNonNull(content, "답변 내용은 필수입니다.");
        this.createdAt = Objects.requireNonNull(createdAt, "작성일은 필수입니다.");
        this.source = Objects.requireNonNull(source, "출처는 필수입니다.");
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
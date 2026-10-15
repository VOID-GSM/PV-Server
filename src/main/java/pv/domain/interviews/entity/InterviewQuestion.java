package pv.domain.interviews.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "interview_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion {

    public enum Major { FRONTEND, BACKEND, DEVOPS, DESIGN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Major major;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewCategory category;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public InterviewQuestion(String content, Major major, InterviewCategory category) {
        this.content = Objects.requireNonNull(content, "질문 내용은 필수입니다.");
        this.major = major;
        this.category = Objects.requireNonNull(category, "카테고리는 필수입니다.");
    }
}

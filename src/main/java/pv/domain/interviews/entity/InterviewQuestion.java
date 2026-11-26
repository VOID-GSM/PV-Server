package pv.domain.interviews.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_question",
        uniqueConstraints = @UniqueConstraint(columnNames = {
                "group_id", "content"
        }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private InterviewGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private Member createdBy;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    private Major major;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewCategory category;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public InterviewQuestion(InterviewGroup group, Member createdBy, String content,
                             Major major, InterviewCategory category) {
        this.group = group;
        this.createdBy = createdBy;
        this.content = content;
        this.major = major;
        this.category = category;
    }
}

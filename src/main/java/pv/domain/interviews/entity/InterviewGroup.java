package pv.domain.interviews.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import pv.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teamId;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private Member createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public InterviewGroup(String name, Member createdBy) {
        this.teamId = createdBy.getTeamId();
        this.name = name;
        this.createdBy = createdBy;
    }

    public void rename(String name) {
        this.name = name;
    }
}

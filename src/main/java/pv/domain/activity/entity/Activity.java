package pv.domain.activity.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import pv.domain.member.entity.Major;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teamId;

    @Column(nullable = false)
    private Long creatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 2000)
    private String description;

    // 대상 전공 / 기수 (비어 있으면 전체 대상)
    @ElementCollection
    @CollectionTable(name = "activity_target_majors", joinColumns = @JoinColumn(name = "activity_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "major")
    @BatchSize(size = 100)
    private Set<Major> targetMajors = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "activity_target_generations", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "generation_id")
    @BatchSize(size = 100)
    private Set<Long> targetGenerationIds = new HashSet<>();

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // 반복 규칙: 매주 repeatDayOfWeek 요일, repeatTime 시각 (기본 수요일)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek repeatDayOfWeek;

    private LocalTime repeatTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitMode recruitMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TopicMode topicMode;

    // 회차 N일 전에 투표 오픈 / 주제 입력 알림
    @Column(nullable = false)
    private int topicOpenDaysBefore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Activity(Long teamId, Long creatorId, ActivityType type, String name, String description,
                    Set<Major> targetMajors, Set<Long> targetGenerationIds, int capacity,
                    LocalDate startDate, LocalDate endDate, DayOfWeek repeatDayOfWeek, LocalTime repeatTime,
                    RecruitMode recruitMode, TopicMode topicMode, int topicOpenDaysBefore) {
        this.teamId = teamId;
        this.creatorId = creatorId;
        this.type = type;
        this.name = name;
        this.description = description;
        if (targetMajors != null) this.targetMajors.addAll(targetMajors);
        if (targetGenerationIds != null) this.targetGenerationIds.addAll(targetGenerationIds);
        this.capacity = capacity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.repeatDayOfWeek = repeatDayOfWeek;
        this.repeatTime = repeatTime;
        this.recruitMode = recruitMode;
        this.topicMode = topicMode;
        this.topicOpenDaysBefore = topicOpenDaysBefore;
        this.status = ActivityStatus.RECRUITING;
    }

    // null이 아닌 값만 수정
    public void update(String name, String description, Set<Major> targetMajors, Set<Long> targetGenerationIds,
                       Integer capacity, LocalDate startDate, LocalDate endDate) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (targetMajors != null) {
            this.targetMajors.clear();
            this.targetMajors.addAll(targetMajors);
        }
        if (targetGenerationIds != null) {
            this.targetGenerationIds.clear();
            this.targetGenerationIds.addAll(targetGenerationIds);
        }
        if (capacity != null) this.capacity = capacity;
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
    }

    public void changeStatus(ActivityStatus status) {
        this.status = status;
    }
}
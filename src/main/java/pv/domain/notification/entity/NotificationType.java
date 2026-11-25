package pv.domain.notification.entity;

public enum NotificationType {
    NOTICE,             // 공지
    DEADLINE_DDAY,      // 마감 D-day
    DEADLINE_ASSIGNED,  // 마감 담당자 지정
    TOPIC_REMINDER,     // 주제 미정 재알림
    UNEXCUSED_ABSENCE,  // 무단 불참
    WEEKLY_REPORT,       // 주간 리포트
    WORKLOG_COMMENT,       //업무일지 댓글
    SUBMISSION_COMMENT,      //제출물 댓글
    NOTE_COMMENT          // 정리글 댓글
}

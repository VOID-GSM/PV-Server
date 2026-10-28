package pv.domain.notification.entity;

// 알림 종류. 새 종류는 여기에 추가한다. (설정은 종류별 수신 여부를 이 값으로 구분한다)
public enum NotificationType {
    NOTICE,             // 공지
    DEADLINE_DDAY,      // 마감 D-day
    TOPIC_REMINDER,     // 주제 미정 재알림
    UNEXCUSED_ABSENCE,  // 무단 불참
    WEEKLY_REPORT       // 주간 리포트
}
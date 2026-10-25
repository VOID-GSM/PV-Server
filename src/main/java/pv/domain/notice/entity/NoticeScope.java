package pv.domain.notice.entity;

public enum NoticeScope {
    ALL,        // 팀 전체
    BOARD,      // 특정 보드 (보드는 팀 단위라 대상은 팀 전체와 같다)
    ACTIVITY,   // 특정 활동 참여자
    TARGET      // 기수 + 전공 (둘 다 만족하는 사람)
}

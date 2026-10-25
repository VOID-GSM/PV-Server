package pv.domain.notice.dto.response;

// 실제로 재알림이 만들어진 인원 수 (알림을 꺼 둔 사람은 제외)
public record NoticeRemindResponse(int remindedCount) {
}

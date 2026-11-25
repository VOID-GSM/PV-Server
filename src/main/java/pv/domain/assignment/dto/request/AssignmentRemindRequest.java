package pv.domain.assignment.dto.request;

// 미제출 알림. 디스코드 웹훅을 선택한 경우에만 디스코드로도 보낸다. (본문 생략 시 false)
public record AssignmentRemindRequest(
        boolean discordEnabled
) {
}

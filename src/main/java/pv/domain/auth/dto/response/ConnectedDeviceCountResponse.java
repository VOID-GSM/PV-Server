package pv.domain.auth.dto.response;

// 내 계정에 로그인된 기기 수 (만료되지 않은 refresh 토큰 수)
public record ConnectedDeviceCountResponse(long count) {
}

package pv.domain.auth.dto.response;

public record TokenRefreshResponse(String accessToken, String refreshToken) {}
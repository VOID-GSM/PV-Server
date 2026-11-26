package pv.domain.auth.dto.response;

import pv.domain.member.entity.MemberStatus;

// reEntryCode: 재입장 코드는 1회용이라 사용 즉시 새로 발급된다. 새 코드를 안내하는 데 사용
public record ReEntryUseResponse(Long memberId, MemberStatus status, String accessToken, String refreshToken,
                                 String reEntryCode) {}
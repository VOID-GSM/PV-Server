package pv.domain.notification.dto.response;

import pv.domain.member.entity.Major;

import java.util.Map;

public record DiscordRoleResponse(
        Map<Long, String> generationRoleIds,
        Map<Major, String> majorRoleIds
) {
}

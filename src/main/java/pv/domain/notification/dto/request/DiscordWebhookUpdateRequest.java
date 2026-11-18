package pv.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DiscordWebhookUpdateRequest(
        @NotBlank(message = "웹훅 URL은 필수입니다.")
        @Pattern(regexp = "^https://(discord|discordapp)\\.com/api/webhooks/\\d+/[\\w-]+$",
                message = "올바른 디스코드 웹훅 URL이 아닙니다.")
        String webhookUrl
) {
}
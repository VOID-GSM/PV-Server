package pv.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "discord_setting", uniqueConstraints = @UniqueConstraint(
        name = "uk_discord_setting_team", columnNames = "team_id"))
public class DiscordSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "webhook_url", nullable = false, length = 300)
    private String webhookUrl;

    public DiscordSetting(Long teamId, String webhookUrl) {
        this.teamId = teamId;
        this.webhookUrl = webhookUrl;
    }

    public void changeWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}
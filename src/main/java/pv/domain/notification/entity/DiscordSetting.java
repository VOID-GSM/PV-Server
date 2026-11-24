package pv.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pv.domain.member.entity.Major;

import java.util.HashMap;
import java.util.Map;

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

    // 기수별 디스코드 역할 ID (key: generation id)
    @ElementCollection
    @CollectionTable(name = "discord_generation_roles", joinColumns = @JoinColumn(name = "discord_setting_id"))
    @MapKeyColumn(name = "generation_id")
    @Column(name = "role_id", nullable = false, length = 20)
    private Map<Long, String> generationRoleIds = new HashMap<>();

    // 전공별 디스코드 역할 ID
    @ElementCollection
    @CollectionTable(name = "discord_major_roles", joinColumns = @JoinColumn(name = "discord_setting_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "major")
    @Column(name = "role_id", nullable = false, length = 20)
    private Map<Major, String> majorRoleIds = new HashMap<>();

    public DiscordSetting(Long teamId, String webhookUrl) {
        this.teamId = teamId;
        this.webhookUrl = webhookUrl;
    }

    public void changeWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    // 보낸 값으로 전체 교체
    public void replaceRoles(Map<Long, String> generationRoleIds, Map<Major, String> majorRoleIds) {
        this.generationRoleIds.clear();
        this.generationRoleIds.putAll(generationRoleIds);
        this.majorRoleIds.clear();
        this.majorRoleIds.putAll(majorRoleIds);
    }
}
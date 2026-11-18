package pv.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.notification.entity.DiscordSetting;

import java.util.Optional;

public interface DiscordSettingRepository extends JpaRepository<DiscordSetting, Long> {

    Optional<DiscordSetting> findByTeamId(Long teamId);
}
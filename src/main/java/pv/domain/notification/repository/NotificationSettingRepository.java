package pv.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.notification.entity.NotificationSetting;
import pv.domain.notification.entity.NotificationType;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByMemberIdAndType(Long memberId, NotificationType type);
    List<NotificationSetting> findAllByMemberId(Long memberId);
}


package pv.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.notification.entity.NotificationSetting;
import pv.domain.notification.entity.NotificationType;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository
        extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findByMemberIdAndType(
            Long memberId, NotificationType type);

    List<NotificationSetting> findAllByMemberId(Long memberId);

    @Modifying
    @Query(value = """
            INSERT INTO notification_setting
                (member_id, notification_type, enabled)
            VALUES
                (:memberId, :type, :enabled)
            ON DUPLICATE KEY UPDATE
                enabled = :enabled
            """, nativeQuery = true)
    int upsertSetting(
            @Param("memberId") Long memberId,
            @Param("type") String type,
            @Param("enabled") boolean enabled
    );
}

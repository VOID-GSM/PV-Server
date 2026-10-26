package pv.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.notification.dto.request.NotificationSettingUpdateRequest;
import pv.domain.notification.dto.response.NotificationListResponse;
import pv.domain.notification.dto.response.NotificationResponse;
import pv.domain.notification.entity.Notification;
import pv.domain.notification.entity.NotificationSetting;
import pv.domain.notification.entity.NotificationType;
import pv.domain.notification.repository.NotificationRepository;
import pv.domain.notification.repository.NotificationSettingRepository;
import pv.global.exception.CustomException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import pv.domain.notification.dto.response.NotificationSettingsResponse;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int MAX_SIZE = 100;
    private static final int TITLE_MAX_LENGTH = 200;

    // IN 절 바인딩 파라미터가 한 쿼리에 너무 많아지지 않게 나눠서 넣는다.
    private static final int INSERT_CHUNK_SIZE = 1000;

    private final Clock clock;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository settingRepository;

    // ===== 내 알림 API =====

    public NotificationListResponse getMine(Long memberId, Long cursor, int size) {
        int limit = Math.min(Math.max(size, 1), MAX_SIZE);
        long from = cursor == null ? Long.MAX_VALUE : cursor;

        // 한 건 더 조회해서 다음 페이지가 있는지 판단한다.
        List<Notification> rows = notificationRepository
                .findByMemberIdAndIdLessThanOrderByIdDesc(memberId, from, PageRequest.of(0, limit + 1));

        boolean hasNext = rows.size() > limit;
        List<Notification> page = hasNext ? rows.subList(0, limit) : rows;

        return new NotificationListResponse(
                page.stream().map(NotificationResponse::from).toList(),
                hasNext ? page.get(page.size() - 1).getId() : null
        );
    }

    // 이미 읽은 알림은 그대로 성공(멱등), 내 알림이 아니거나 없으면 404
    @Transactional
    public void markRead(Long memberId, Long notificationId) {
        int updated = notificationRepository.markRead(notificationId, memberId, LocalDateTime.now(clock));

        if (updated == 0 && !notificationRepository.existsByIdAndMemberId(notificationId, memberId)) {
            throw new CustomException(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다.");
        }
    }

    @Transactional
    public void markAllRead(Long memberId) {
        notificationRepository.markAllRead(memberId, LocalDateTime.now(clock));
    }


    // 모든 알림 유형의 설정 조회. 저장된 설정이 없으면 기본값 true
    public NotificationSettingsResponse getSettings(Long memberId) {
        Map<NotificationType, Boolean> savedSettings =
                settingRepository.findAllByMemberId(memberId)
                        .stream()
                        .collect(Collectors.toMap(
                                NotificationSetting::getType,
                                NotificationSetting::isEnabled
                        ));

        List<NotificationSettingsResponse.SettingItem> settings =
                java.util.Arrays.stream(NotificationType.values())
                        .map(type -> new NotificationSettingsResponse.SettingItem(
                                type,
                                savedSettings.getOrDefault(type, true)
                        ))
                        .toList();

        return new NotificationSettingsResponse(settings);
    }



    // 설정이 없으면 생성하고, 있으면 갱신한다.
    // DB upsert로 동시 요청 시 유니크 제약 위반을 방지한다.
    @Transactional
    public void updateSetting(Long memberId, NotificationSettingUpdateRequest request) {
        settingRepository.upsertSetting(
                memberId,
                request.type().name(),
                request.enabled()
        );
    }


    // ===== 다른 도메인(공지 등)에서 쓰는 메서드 =====

    // 호출한 쪽의 트랜잭션에 합류한다. 알림 실패 시 호출한 쪽도 함께 롤백된다.
    // 해당 종류를 끈 사람은 제외되며, 실제로 만들어진 알림 수를 반환한다.
    @Transactional
    public int send(NotificationType type, String title, Long targetId, Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return 0;
        }

        String safeTitle = title.length() > TITLE_MAX_LENGTH ? title.substring(0, TITLE_MAX_LENGTH) : title;
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(memberIds));

        int created = 0;
        for (int i = 0; i < ids.size(); i += INSERT_CHUNK_SIZE) {
            List<Long> chunk = ids.subList(i, Math.min(i + INSERT_CHUNK_SIZE, ids.size()));
            created += notificationRepository.insertForMembers(type.name(), safeTitle, targetId, now, chunk);
        }
        return created;
    }

    // 공지를 읽으면 그 공지의 알림도 읽음 처리
    @Transactional
    public void markTargetRead(Long memberId, NotificationType type, Long targetId) {
        notificationRepository.markTargetRead(memberId, type, targetId, LocalDateTime.now(clock));
    }

    // 공지가 삭제되면 가리키는 대상이 없는 알림도 함께 지운다.
    @Transactional
    public void deleteByTarget(NotificationType type, Long targetId) {
        notificationRepository.deleteByTarget(type, targetId);
    }
}

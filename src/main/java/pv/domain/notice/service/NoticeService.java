package pv.domain.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.activity.entity.ActivityRole;
import pv.domain.activity.repository.ActivityMemberRepository;
import pv.domain.activity.repository.ActivityRepository;
import pv.domain.board.repository.BoardRepository;
import pv.domain.member.entity.Generation;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.repository.GenerationRepository;
import pv.domain.member.repository.MemberRepository;
import pv.domain.notice.dto.query.NoticeSummaryRow;
import pv.domain.notice.dto.request.NoticeCreateRequest;
import pv.domain.notice.dto.request.NoticeUpdateRequest;
import pv.domain.notice.dto.response.*;
import pv.domain.notice.entity.Notice;
import pv.domain.notice.entity.NoticeScope;
import pv.domain.notice.repository.NoticeRecipientRepository;
import pv.domain.notice.repository.NoticeRepository;
import pv.domain.notification.entity.NotificationType;
import pv.domain.notification.event.DiscordNoticeEvent;
import pv.domain.notification.service.NotificationService;
import pv.global.exception.CustomException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private static final int MAX_SIZE = 100;

    // 같은 공지에 대한 재알림 최소 간격 (알림 연타 방지)
    private static final Duration REMIND_INTERVAL = Duration.ofHours(1);

    // IN 절 바인딩 파라미터가 한 쿼리에 너무 많아지지 않게 나눠서 넣는다.
    private static final int INSERT_CHUNK_SIZE = 1000;

    private final Clock clock;
    private final NoticeRepository noticeRepository;
    private final NoticeRecipientRepository recipientRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final ActivityRepository activityRepository;
    private final ActivityMemberRepository activityMemberRepository;
    private final GenerationRepository generationRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    // ===== 목록 / 상세 =====

    // 내가 대상인 공지만 최신순으로 조회한다. (수신자 row가 있는 사람만 결과가 나오므로 별도 멤버 조회가 필요 없다)
    public NoticeListResponse getNotices(Long loginMemberId, Long cursor, int size) {
        int limit = Math.min(Math.max(size, 1), MAX_SIZE);
        long from = cursor == null ? Long.MAX_VALUE : cursor;

        // 한 건 더 조회해서 다음 페이지가 있는지 판단한다.
        List<NoticeSummaryRow> rows = recipientRepository
                .findSummaries(loginMemberId, from, PageRequest.of(0, limit + 1));

        boolean hasNext = rows.size() > limit;
        List<NoticeSummaryRow> page = hasNext ? rows.subList(0, limit) : rows;

        return new NoticeListResponse(
                page.stream().map(NoticeSummaryResponse::from).toList(),
                hasNext ? page.get(page.size() - 1).id() : null
        );
    }

    // 작성자와 대상자만 볼 수 있다. 그 외에는 존재하지 않는 공지처럼 404
    public NoticeDetailResponse getNotice(Long loginMemberId, Long noticeId) {
        Notice notice = findNotice(noticeId);

        boolean read;
        if (notice.isAuthor(loginMemberId)) {
            read = true;
        } else {
            read = recipientRepository.findByNoticeIdAndMemberId(noticeId, loginMemberId)
                    .orElseThrow(NoticeService::noticeNotFound)
                    .isRead();
        }

        String authorName = memberRepository.findNameById(notice.getAuthorId()).orElse(null);
        return NoticeDetailResponse.of(notice, authorName, read);
    }

    // ===== 작성 / 수정 / 삭제 =====

    // 대상자를 작성 시점에 한 번 계산해서 수신자 row와 알림을 각각 한 쿼리로 저장한다.
    @Transactional
    public NoticeDetailResponse create(Long loginMemberId, NoticeCreateRequest request) {
        Member me = getMember(loginMemberId);
        NoticeScope scope = request.scope();

        Long boardId = null;
        Long activityId = null;
        Set<Long> generationIds = Set.of();
        Set<Major> majors = Set.of();

        switch (scope) {
            case ALL -> requireLeader(me);
            case BOARD -> {
                requireLeader(me);
                boardId = requireNotNull(request.boardId(), "보드를 지정해야 합니다.");
                requireBoardOfTeam(me, boardId);
            }
            case ACTIVITY -> {
                activityId = requireNotNull(request.activityId(), "활동을 지정해야 합니다.");
                requireActivityOfTeam(me, activityId);
                requireLeaderOrActivityLeader(me, activityId);
            }
            case TARGET -> {
                requireLeader(me);
                generationIds = request.generationIds();
                majors = request.majors();
                if (generationIds == null || generationIds.isEmpty() || majors == null || majors.isEmpty()) {
                    throw error(HttpStatus.BAD_REQUEST, "기수와 전공을 모두 지정해야 합니다.");
                }
                validateGenerations(me, generationIds);
            }
        }

        Notice notice = noticeRepository.save(new Notice(
                me.getTeamId(), me.getId(), scope, boardId, activityId, generationIds, majors,
                request.title().trim(), request.content().trim(), LocalDateTime.now(clock)));

        List<Long> recipientIds = resolveRecipients(me, scope, activityId, generationIds, majors);
        insertRecipients(notice.getId(), recipientIds);

        notificationService.send(
                NotificationType.NOTICE, "[공지] " + notice.getTitle(), notice.getId(), recipientIds);

        // 팀 전체 대상(ALL) 공지만 디스코드 채널에 올린다. (일부에게만 가는 공지가 전체 채널에 노출되지 않도록)
        if (scope == NoticeScope.ALL) {
            eventPublisher.publishEvent(new DiscordNoticeEvent(
                    me.getTeamId(), "📢 [공지] " + notice.getTitle()));
        }

        return NoticeDetailResponse.of(notice, me.getName(), true);
    }

    // 제목/내용만 수정할 수 있다. (대상자는 작성 시점에 확정되므로 범위는 바꿀 수 없다)
    @Transactional
    public void update(Long loginMemberId, Long noticeId, NoticeUpdateRequest request) {
        if (request.title() == null && request.content() == null) {
            throw error(HttpStatus.BAD_REQUEST, "수정할 내용이 없습니다.");
        }

        Notice notice = findNoticeForUpdate(noticeId);
        requireAuthor(notice, loginMemberId);

        notice.update(
                request.title() == null ? null : request.title().trim(),
                request.content() == null ? null : request.content().trim(),
                LocalDateTime.now(clock));
    }

    // 수신자와 알림을 벌크 쿼리로 먼저 지운 뒤 공지를 삭제한다.
    @Transactional
    public void delete(Long loginMemberId, Long noticeId) {
        Notice notice = findNoticeForUpdate(noticeId);
        requireAuthor(notice, loginMemberId);

        recipientRepository.deleteAllByNoticeId(noticeId);
        notificationService.deleteByTarget(NotificationType.NOTICE, noticeId);
        noticeRepository.delete(notice);
    }

    // ===== 읽음 =====

    // 멱등: 이미 읽은 공지는 그대로 성공한다. 작성자는 읽음 대상이 아니므로 조용히 성공한다.
    @Transactional
    public void markRead(Long loginMemberId, Long noticeId) {
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = recipientRepository.markRead(noticeId, loginMemberId, now);

        if (updated == 0 && !recipientRepository.existsByNoticeIdAndMemberId(noticeId, loginMemberId)) {
            boolean author = noticeRepository.findById(noticeId)
                    .map(n -> n.isAuthor(loginMemberId))
                    .orElse(false);
            if (author) {
                return;
            }
            throw noticeNotFound();
        }

        // 이 공지에 대한 알림도 함께 읽음 처리
        notificationService.markTargetRead(loginMemberId, NotificationType.NOTICE, noticeId);
    }

    // 작성자만 조회할 수 있다. 대상자 이름과 읽은 시각을 한 번의 쿼리로 가져온다.
    public NoticeReadStatusResponse getReadStatus(Long loginMemberId, Long noticeId) {
        Notice notice = findNotice(noticeId);
        requireAuthor(notice, loginMemberId);

        return NoticeReadStatusResponse.from(recipientRepository.findReadRows(noticeId));
    }

    // 안 읽은 대상자에게만 알림을 다시 보낸다.
    // 공지 행을 잠가서 동시에 여러 번 눌러도 간격 제한이 정확히 적용된다.
    @Transactional
    public NoticeRemindResponse remind(Long loginMemberId, Long noticeId) {
        Notice notice = findNoticeForUpdate(noticeId);
        requireAuthor(notice, loginMemberId);

        LocalDateTime now = LocalDateTime.now(clock);

        Duration remaining = notice.remainingRemindCooldown(now, REMIND_INTERVAL);
        if (!remaining.isZero()) {
            long minutes = remaining.toMinutes() + 1;
            throw error(HttpStatus.TOO_MANY_REQUESTS,
                    "재알림은 1시간에 한 번만 보낼 수 있습니다. 약 " + minutes + "분 후에 다시 시도해주세요.");
        }

        List<Long> unreadIds = recipientRepository.findUnreadMemberIds(noticeId);
        if (unreadIds.isEmpty()) {
            return new NoticeRemindResponse(0);
        }

        notice.markReminded(now);
        int reminded = notificationService.send(
                NotificationType.NOTICE, "[재알림] " + notice.getTitle(), noticeId, unreadIds);

        return new NoticeRemindResponse(reminded);
    }

    // ===== 공통 =====

    // 가입 완료(JOINED)된 팀원만 작성 가능
    private Member getMember(Long loginMemberId) {
        Member member = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "존재하지 않는 멤버입니다."));

        if (member.getStatus() != MemberStatus.JOINED) {
            throw error(HttpStatus.FORBIDDEN, "가입이 완료된 팀원만 사용할 수 있습니다.");
        }
        return member;
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId).orElseThrow(NoticeService::noticeNotFound);
    }

    private Notice findNoticeForUpdate(Long noticeId) {
        return noticeRepository.findWithLockById(noticeId).orElseThrow(NoticeService::noticeNotFound);
    }

    // 작성자만 허용. 대상자에게는 권한 없음(403)을, 관련 없는 사람에게는 존재하지 않는 공지(404)처럼 보이게 한다.
    private void requireAuthor(Notice notice, Long memberId) {
        if (notice.isAuthor(memberId)) {
            return;
        }
        if (recipientRepository.existsByNoticeIdAndMemberId(notice.getId(), memberId)) {
            throw error(HttpStatus.FORBIDDEN, "작성자만 사용할 수 있는 기능입니다.");
        }
        throw noticeNotFound();
    }

    private void requireLeader(Member me) {
        if (!me.isLeader()) {
            throw error(HttpStatus.FORBIDDEN, "부장만 해당 범위로 공지할 수 있습니다.");
        }
    }

    // 부장은 모든 활동, 스터디장은 자기 활동에만 공지할 수 있다.
    private void requireLeaderOrActivityLeader(Member me, Long activityId) {
        if (me.isLeader()) {
            return;
        }
        if (!activityMemberRepository.existsByActivityIdAndMemberIdAndRole(
                activityId, me.getId(), ActivityRole.LEADER)) {
            throw error(HttpStatus.FORBIDDEN, "스터디장 또는 부장만 공지할 수 있습니다.");
        }
    }

    // 다른 팀의 보드/활동은 존재하지 않는 것처럼 404
    private void requireBoardOfTeam(Member me, Long boardId) {
        boolean exists = boardRepository.findById(boardId)
                .filter(b -> b.getTeamId().equals(me.getTeamId()))
                .isPresent();
        if (!exists) {
            throw error(HttpStatus.NOT_FOUND, "존재하지 않는 보드입니다.");
        }
    }

    private void requireActivityOfTeam(Member me, Long activityId) {
        boolean exists = activityRepository.findById(activityId)
                .filter(a -> a.getTeamId().equals(me.getTeamId()))
                .isPresent();
        if (!exists) {
            throw error(HttpStatus.NOT_FOUND, "존재하지 않는 활동입니다.");
        }
    }

    // 기수는 같은 팀의 기수만 지정 가능. 한 번의 쿼리로 검증한다.
    private void validateGenerations(Member me, Set<Long> generationIds) {
        List<Generation> found = generationRepository.findAllById(generationIds);

        boolean valid = found.size() == generationIds.size()
                && found.stream().allMatch(g -> g.getTeamId().equals(me.getTeamId()));

        if (!valid) {
            throw error(HttpStatus.BAD_REQUEST, "존재하지 않는 기수입니다.");
        }
    }

    // 범위별 대상자 ID 계산 (작성자 본인은 제외). 항상 ID만 조회한다.
    private List<Long> resolveRecipients(Member me, NoticeScope scope, Long activityId,
                                         Set<Long> generationIds, Set<Major> majors) {
        List<Long> ids = switch (scope) {
            case ALL, BOARD -> memberRepository.findJoinedIdsByTeamId(me.getTeamId());
            case ACTIVITY -> {
                List<Long> participantIds = activityMemberRepository.findMemberIdsByActivityId(activityId);
                yield participantIds.isEmpty()
                        ? List.<Long>of()
                        : memberRepository.findJoinedIds(me.getTeamId(), participantIds);
            }
            case TARGET -> memberRepository.findJoinedIdsByGenerationsAndMajors(
                    me.getTeamId(), generationIds, majors);
        };

        return ids.stream().filter(id -> !id.equals(me.getId())).toList();
    }

    private void insertRecipients(Long noticeId, List<Long> memberIds) {
        for (int i = 0; i < memberIds.size(); i += INSERT_CHUNK_SIZE) {
            recipientRepository.insertAll(
                    noticeId, memberIds.subList(i, Math.min(i + INSERT_CHUNK_SIZE, memberIds.size())));
        }
    }

    private static <T> T requireNotNull(T value, String message) {
        if (value == null) {
            throw error(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    private static CustomException noticeNotFound() {
        return error(HttpStatus.NOT_FOUND, "존재하지 않는 공지입니다.");
    }

    private static CustomException error(HttpStatus status, String message) {
        return new CustomException(status, message);
    }
}
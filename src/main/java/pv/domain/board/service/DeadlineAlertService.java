package pv.domain.board.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pv.domain.board.entity.Board;
import pv.domain.board.entity.BoardStatus;
import pv.domain.board.entity.Deadline;
import pv.domain.board.entity.DeadlineAlertLog;
import pv.domain.board.repository.BoardRepository;
import pv.domain.board.repository.DeadlineAlertLogRepository;
import pv.domain.board.repository.DeadlineRepository;
import pv.domain.notification.entity.NotificationType;
import pv.domain.notification.service.NotificationService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeadlineAlertService {
    private final Clock clock;
    private final BoardRepository boardRepository;
    private final DeadlineRepository deadlineRepository;
    private final DeadlineAlertLogRepository alertLogRepository;
    private final NotificationService notificationService;
    private final TransactionTemplate readTx;
    private final TransactionTemplate writeTx;

    public DeadlineAlertService(Clock clock,
                                BoardRepository boardRepository,
                                DeadlineRepository deadlineRepository,
                                DeadlineAlertLogRepository alertLogRepository,
                                NotificationService notificationService,
                                PlatformTransactionManager transactionManager) {
        this.clock = clock;
        this.boardRepository = boardRepository;
        this.deadlineRepository = deadlineRepository;
        this.alertLogRepository = alertLogRepository;
        this.notificationService = notificationService;
        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
        this.writeTx = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public void notifyAssigned(Board board, Deadline deadline, Collection<Long> memberIds, Long actorId) {
        if (memberIds == null || memberIds.isEmpty()) return;

        Set<Long> ids = new LinkedHashSet<>(memberIds);
        ids.remove(actorId);
        if (ids.isEmpty()) return;

        String title = "[새 마감] " + board.getName() + " · " + deadline.getName()
            + " (" + deadline.getDeadlineDate() + ")";
        notificationService.send(NotificationType.DEADLINE_ASSIGNED, title, deadline.getId(), ids);
    }

    @Transactional
    public void deleteAlerts(Collection<Long> deadlineIds) {
        if (deadlineIds.isEmpty()) return;
        notificationService.deleteByTargets(
            List.of(NotificationType.DEADLINE_ASSIGNED, NotificationType.DEADLINE_DDAY), deadlineIds);
        alertLogRepository.deleteAllByDeadlineIds(deadlineIds);
    }

    public int sendDailyAlerts() {
        LocalDate today = LocalDate.now(clock);
        List<AlertTarget> targets = readTx.execute(status -> findTargets(today));
        if (targets == null || targets.isEmpty()) return 0;

        int sent = 0;
        for (AlertTarget t : targets) {
            try {
                writeTx.executeWithoutResult(status -> sendOne(t));
                sent++;
            } catch (DataIntegrityViolationException e) {
                log.info("이미 발송된 마감 D-day 알림: deadlineId={}, D-{}", t.deadlineId(), t.daysLeft());
            } catch (RuntimeException e) {
                log.warn("마감 D-day 알림 발송 실패: deadlineId={}, D-{}", t.deadlineId(), t.daysLeft(), e);
            }
        }
        return sent;
    }

    private record AlertTarget(Long deadlineId, LocalDate deadlineDate, int daysLeft,
                               String title, Set<Long> assigneeIds) {
        String key() {
            return key(deadlineId, deadlineDate, daysLeft);
        }

        static String key(Long deadlineId, LocalDate deadlineDate, int daysLeft) {
            return deadlineId + ":" + deadlineDate + ":" + daysLeft;
        }
    }

    private List<AlertTarget> findTargets(LocalDate today) {
        List<Board> boards = boardRepository.findAllByStatus(BoardStatus.ACTIVE).stream()
            .filter(b -> !b.getAlertDays().isEmpty())
            .toList();
        if (boards.isEmpty()) return List.of();

        Map<Long, Board> boardById = boards.stream()
            .collect(Collectors.toMap(Board::getId, Function.identity()));
        Set<LocalDate> targetDates = boards.stream()
            .flatMap(b -> b.getAlertDays().stream())
            .map(today::plusDays)
            .collect(Collectors.toSet());

        List<AlertTarget> candidates = new ArrayList<>();
        for (Deadline d : deadlineRepository.findAllByBoardIdInAndDeadlineDateIn(boardById.keySet(), targetDates)) {
            Board board = boardById.get(d.getBoardId());
            int daysLeft = (int) ChronoUnit.DAYS.between(today, d.getDeadlineDate());
            if (!board.getAlertDays().contains(daysLeft) || d.getAssigneeIds().isEmpty()) continue;

            String label = daysLeft == 0 ? "[D-day]" : "[D-" + daysLeft + "]";
            candidates.add(new AlertTarget(d.getId(), d.getDeadlineDate(), daysLeft,
                label + " " + board.getName() + " · " + d.getName(), Set.copyOf(d.getAssigneeIds())));
        }
        if (candidates.isEmpty()) return List.of();

        Set<String> alreadySent = alertLogRepository
            .findAllByDeadlineIdIn(candidates.stream().map(AlertTarget::deadlineId).toList()).stream()
            .map(l -> AlertTarget.key(l.getDeadlineId(), l.getDeadlineDate(), l.getDaysLeft()))
            .collect(Collectors.toSet());

        return candidates.stream()
            .filter(t -> !alreadySent.contains(t.key()))
            .toList();
    }

    private void sendOne(AlertTarget t) {
        alertLogRepository.saveAndFlush(
            new DeadlineAlertLog(t.deadlineId(), t.deadlineDate(), t.daysLeft(), LocalDateTime.now(clock)));
        notificationService.send(NotificationType.DEADLINE_DDAY, t.title(), t.deadlineId(), t.assigneeIds());
    }
}

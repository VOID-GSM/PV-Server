package pv.domain.board.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import pv.domain.board.entity.Board;
import pv.domain.board.entity.BoardStatus;
import pv.domain.board.entity.Deadline;
import pv.domain.board.repository.BoardRepository;
import pv.domain.notification.entity.NotificationType;
import pv.domain.notification.event.DeadlineAlertEvent;
import pv.domain.notification.service.NotificationService;
import pv.domain.board.repository.DeadlineRepository;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeadlineAlertService {
    private final Clock clock;
    private final BoardRepository boardRepository;
    private final DeadlineRepository deadlineRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void notifyAssigned(Board board, Deadline deadline, Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return;

        Set<Long> ids = new LinkedHashSet<>(memberIds);
        String title = "[새 마감] " + board.getName() + " · " + deadline.getName()
                + " (" + deadline.getDeadlineDate() + ")";
        notificationService.send(NotificationType.DEADLINE_DDAY, title, deadline.getId(), ids);
        eventPublisher.publishEvent(new DeadlineAlertEvent(
                DeadlineAlertEvent.Kind.ASSIGNED, board.getTeamId(), board.getId(), board.getName(),
                deadline.getId(), deadline.getName(), deadline.getDeadlineDate(), null, Set.copyOf(ids)));
    }

    @Transactional
    public int sendDailyAlerts() {
        LocalDate today = LocalDate.now(clock);

        List<Board> boards = boardRepository.findAllByStatus(BoardStatus.ACTIVE).stream()
                .filter(b -> !b.getAlertDays().isEmpty())
                .toList();
        if (boards.isEmpty()) return 0;

        Map<Long, Board> boardById = boards.stream()
                .collect(Collectors.toMap(Board::getId, Function.identity()));
        Set<LocalDate> targetDates = boards.stream()
                .flatMap(b -> b.getAlertDays().stream())
                .map(today::plusDays)
                .collect(Collectors.toSet());

        int sent = 0;
        for (Deadline d : deadlineRepository.findAllByBoardIdInAndDeadlineDateIn(boardById.keySet(), targetDates)) {
            Board board = boardById.get(d.getBoardId());
            int daysLeft = (int) ChronoUnit.DAYS.between(today, d.getDeadlineDate());
            if (!board.getAlertDays().contains(daysLeft) || d.getAssigneeIds().isEmpty()) continue;

            String label = daysLeft == 0 ? "[D-day]" : "[D-" + daysLeft + "]";
            notificationService.send(NotificationType.DEADLINE_DDAY,
                    label + " " + board.getName() + " · " + d.getName(), d.getId(), d.getAssigneeIds());
            eventPublisher.publishEvent(new DeadlineAlertEvent(
                    DeadlineAlertEvent.Kind.D_DAY, board.getTeamId(), board.getId(), board.getName(),
                    d.getId(), d.getName(), d.getDeadlineDate(), daysLeft, Set.copyOf(d.getAssigneeIds())));
            sent++;
        }
        return sent;
    }
}

package pv.domain.attendance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pv.domain.attendance.entity.AttendanceSession;
import pv.domain.attendance.repository.AttendanceSessionRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceCloseScheduler {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceService attendanceService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void closeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now(ZONE);
        for (AttendanceSession session : sessionRepository.findAllByClosedFalseAndCheckInClosesAtBefore(now)) {
            try {
                int absentCount = attendanceService.closeSession(session.getSessionId());
                log.info("출석 마감 완료 - sessionId={}, 무단 불참 처리={}명", session.getSessionId(), absentCount);
            } catch (Exception e) {
                log.error("출석 마감 실패 - sessionId={}", session.getSessionId(), e);
            }
        }
    }
}

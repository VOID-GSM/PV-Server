package pv.domain.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.attendance.entity.AttendanceSession;

import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findAllByClosedFalseAndCheckInClosesAtBefore(LocalDateTime now);
}
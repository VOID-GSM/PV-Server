package pv.domain.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.attendance.entity.AttendanceSession;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
}
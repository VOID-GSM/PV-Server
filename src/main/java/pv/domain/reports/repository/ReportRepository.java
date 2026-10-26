package pv.domain.reports.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.reports.entity.Report;

import java.time.LocalDate;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByTeamIdAndWeekStart(Long teamId, LocalDate weekStart);
}

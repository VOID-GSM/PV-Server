package pv.domain.reports.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.member.entity.Member;
import pv.domain.member.repository.MemberRepository;
import pv.domain.reports.dto.ReportResponse;
import pv.domain.reports.entity.Report;
import pv.domain.reports.repository.ReportRepository;
import pv.domain.worklogs.entity.Visibility;
import pv.domain.worklogs.repository.WorklogRepository;
import pv.global.exception.CustomException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final ReportRepository reportRepository;
    private final WorklogRepository worklogRepository;
    private final MemberRepository memberRepository;
    private final ReportSummarizer summarizer;

    @Transactional(readOnly = true)
    public ReportResponse get(Long memberId, LocalDate date) {
        Member member = joinedMember(memberId);
        return reportRepository.findByTeamIdAndWeekStart(member.getTeamId(), weekStart(date))
                .map(ReportResponse::from)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "리포트가 아직 생성되지 않았습니다."));
    }

    public ReportResponse generate(Long memberId, LocalDate date) {
        Member member = joinedMember(memberId);
        if (!member.isLeader()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "부장만 리포트를 생성할 수 있습니다.");
        }
        Long teamId = member.getTeamId();
        if (teamId == null) {
            throw new CustomException(HttpStatus.FORBIDDEN, "팀에 소속되지 않은 회원입니다.");
        }
        LocalDate start = weekStart(date);
        List<String> sources = worklogRepository.findForReport(
                teamId, Visibility.PUBLIC, start.atStartOfDay(), start.plusDays(7).atStartOfDay())
                .stream()
                .map(w -> "[" + w.getAuthor().getName() + "] " + w.getTitle() + "\n" + w.getContent())
                .toList();
        String content = summarizer.summarize(sources);
        Report report = reportRepository.findByTeamIdAndWeekStart(teamId, start)
                .map(existing -> {
                    existing.update(content);
                    return existing;
                })
                .orElseGet(() -> new Report(teamId, start, content));
        try {
            return ReportResponse.from(reportRepository.save(report));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(HttpStatus.CONFLICT, "리포트를 생성하는 중입니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private Member joinedMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "회원을 찾을 수 없습니다."));
        if (!member.isJoined()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "가입 승인 대기 중입니다.");
        }
        if (member.getTeamId() == null) {
            throw new CustomException(HttpStatus.FORBIDDEN, "팀에 소속되지 않은 회원입니다.");
        }
        return member;
    }

    private LocalDate weekStart(LocalDate date) {
        LocalDate d = (date != null) ? date : LocalDate.now(KST);
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}

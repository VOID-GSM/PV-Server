package pv.domain.reports.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.reports.dto.ReportResponse;
import pv.domain.reports.service.ReportService;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports/weekly")
public class ReportController {
    private final ReportService reportService;

    @GetMapping
    public ReportResponse get(@AuthenticationPrincipal Long memberId,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportService.get(memberId, date);
    }

    @PostMapping("/generate")
    public ReportResponse generate(@AuthenticationPrincipal Long memberId,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportService.generate(memberId, date);
    }
}

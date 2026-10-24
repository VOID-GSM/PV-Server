package pv.domain.reports.service;

import java.util.List;
import java.util.stream.Collectors;

public class StubReportSummarizer implements ReportSummarizer {
    @Override
    public String summarize(List<String> sources) {
        if (sources.isEmpty()) return "이번 주 공개 업무 일지가 없습니다.";
        return sources.stream().map(s -> "- " + s).collect(Collectors.joining("\n\n"));
    }
}

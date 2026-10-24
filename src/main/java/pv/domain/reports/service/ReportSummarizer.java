package pv.domain.reports.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface ReportSummarizer {
    String summarize(List<String > sources);
}

package pv.domain.activity.dto.response;

import pv.domain.activity.entity.Activity;
import pv.domain.activity.entity.TopicMode;
import pv.domain.member.entity.Major;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record ActivityDetailResponse(
        ActivitySummaryResponse summary,
        String description,
        List<Major> targetMajors,
        List<GenerationItem> targetGenerations,
        TopicMode topicMode, String topicModeLabel,
        DayOfWeek repeatDayOfWeek, LocalTime repeatTime,
        int topicOpenDaysBefore,
        List<ActivityMemberResponse> members,
        List<SessionResponse> upcomingSessions
) {
    public record GenerationItem(Long id, Integer number) {
    }

    public static ActivityDetailResponse of(Activity a, ActivitySummaryResponse summary,
                                            List<GenerationItem> targetGenerations,
                                            List<ActivityMemberResponse> members,
                                            List<SessionResponse> upcomingSessions) {
        return new ActivityDetailResponse(summary, a.getDescription(),
                a.getTargetMajors().stream().sorted().toList(), targetGenerations,
                a.getTopicMode(), a.getTopicMode().getLabel(),
                a.getRepeatDayOfWeek(), a.getRepeatTime(), a.getTopicOpenDaysBefore(),
                members, upcomingSessions);
    }
}
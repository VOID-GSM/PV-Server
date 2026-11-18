package pv.domain.activity.dto.request;

import jakarta.validation.constraints.*;
import pv.domain.activity.entity.ActivityType;
import pv.domain.activity.entity.RecruitMode;
import pv.domain.activity.entity.TopicMode;
import pv.domain.member.entity.Major;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record ActivityCreateRequest(
        @NotNull ActivityType type,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @Size(max = 10) Set<Major> targetMajors,
        @Size(max = 20) Set<Long> targetGenerationIds,
        @NotNull @Min(1) @Max(200) Integer capacity,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        DayOfWeek repeatDayOfWeek,
        LocalTime repeatTime,
        @NotNull RecruitMode recruitMode,
        @NotNull TopicMode topicMode,
        @Min(0) @Max(30) Integer topicOpenDaysBefore
) {
}
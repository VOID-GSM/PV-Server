package pv.domain.activity.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import pv.domain.activity.entity.ActivityStatus;
import pv.domain.member.entity.Major;

import java.util.Set;

public record ActivityUpdateRequest(
        @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @Size(max = 10) Set<Major> targetMajors,
        @Size(max = 20) Set<Long> targetGenerationIds,
        @Min(1) @Max(200) Integer capacity,
        ActivityStatus status
) {
}
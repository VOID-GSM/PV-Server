package pv.domain.attendance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import pv.domain.attendance.entity.AttendanceStatus;

import java.util.List;

public record BulkAttendanceRequest(@NotEmpty List<@Valid Item> items) {
    public record Item(@NotNull Long memberId, @NotNull AttendanceStatus status) {}
}

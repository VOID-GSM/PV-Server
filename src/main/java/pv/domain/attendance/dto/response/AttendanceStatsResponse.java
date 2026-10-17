package pv.domain.attendance.dto.response;

import pv.domain.attendance.dto.StatusCount;
import pv.domain.attendance.entity.AttendanceStatus;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AttendanceStatsResponse(Long targetId, long present, long excused, long absent, double rate) {

    public static List<AttendanceStatsResponse> from(List<StatusCount> counts) {
        Map<Long, EnumMap<AttendanceStatus, Long>> grouped = new LinkedHashMap<>();
        for (StatusCount c : counts) {
            grouped.computeIfAbsent(c.groupId(), k -> new EnumMap<>(AttendanceStatus.class))
                    .put(c.status(), c.count());
        }
        return grouped.entrySet().stream().map(e -> {
            long p = e.getValue().getOrDefault(AttendanceStatus.PRESENT, 0L);
            long ex = e.getValue().getOrDefault(AttendanceStatus.EXCUSED, 0L);
            long ab = e.getValue().getOrDefault(AttendanceStatus.ABSENT, 0L);
            long total = p + ex + ab;

            double rate = total == 0 ? 0 : Math.round(p * 1000.0 / total) / 10.0;
            return new AttendanceStatsResponse(e.getKey(), p, ex, ab, rate);
        }).toList();
    }
}

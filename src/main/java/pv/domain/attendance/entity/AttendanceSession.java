package pv.domain.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceSession {
    @Id
    private Long sessionId;

    @Column(nullable = false)
    private Long activityId;

    private String checkInCode;

    @Column(nullable = false)
    private LocalDateTime checkInOpensAt;

    @Column(nullable = false)
    private LocalDateTime checkInClosesAt;

    public AttendanceSession(Long sessionId, Long activityId, String checkInCode,
                             LocalDateTime opensAt, LocalDateTime closesAt) {
        this.sessionId = sessionId;
        this.activityId = activityId;
        this.checkInCode = checkInCode;
        this.checkInOpensAt = opensAt;
        this.checkInClosesAt = closesAt;
    }

    public boolean isCheckInOpen(LocalDateTime now) {
        return !now.isBefore(checkInOpensAt) && now.isBefore(checkInClosesAt);
    }

    public boolean matchesCode(String code) {
        return checkInCode == null || checkInCode.equals(code);
    }

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean closed = false;

    public void close() {
        this.closed = true;
    }
}

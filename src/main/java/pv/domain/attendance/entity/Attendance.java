package pv.domain.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import pv.global.exception.CustomException;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "member_id"}),
        indexes = @Index(columnList = "member_id, activity_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AttendanceStatus status = AttendanceStatus.PENDING;

    private LocalDateTime checkedAt;

    public void checkIn(LocalDateTime now) {
        if (status != AttendanceStatus.PENDING) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 출석 처리된 회차입니다.");
        }
        this.status = AttendanceStatus.PENDING;
        this.checkedAt = now;
    }

    public void changeStatus(AttendanceStatus status, LocalDateTime now) {
        this.status = status;
        this.checkedAt = status == AttendanceStatus.PRESENT ? now : null;
    }
}

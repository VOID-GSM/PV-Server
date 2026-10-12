package pv.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long teamId;

    @Column(unique = true, nullable = false)
    private String code;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt; // null이면 무기한

    private Integer maxUses; // null이면 무제한

    private int useCount;

    public Invite(Long teamId, String code, LocalDateTime expiresAt, Integer maxUses) {
        this.teamId = teamId;
        this.code = code;
        this.expiresAt = expiresAt;
        this.maxUses = maxUses;
        this.useCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isExhausted() {
        return maxUses != null && useCount >= maxUses;
    }

    public void use() {
        this.useCount++;
    }
}
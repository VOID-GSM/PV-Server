package pv.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReEntryLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long memberId;

    @Column(unique = true, nullable = false)
    private String code;

    private LocalDateTime createdAt;

    public ReEntryLink(Long memberId, String code) {
        this.memberId = memberId;
        this.code = code;
        this.createdAt = LocalDateTime.now();
    }

    public void regenerate(String code) {
        this.code = code;
        this.createdAt = LocalDateTime.now();
    }
}
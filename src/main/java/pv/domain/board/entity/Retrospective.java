package pv.domain.board.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Retrospective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long boardId;

    private Long memberId;

    // 참여자가 나중에 작성 (자동 생성 시점에는 비어 있음)
    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt;

    public Retrospective(Long boardId, Long memberId) {
        this.boardId = boardId;
        this.memberId = memberId;
        this.createdAt = LocalDateTime.now();
    }
}

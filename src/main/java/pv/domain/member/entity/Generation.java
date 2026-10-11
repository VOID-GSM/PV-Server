package pv.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Generation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long teamId;

    @Column(name = "generation_number")
    private Integer number; // 9, 10, 11 ...

    public Generation(Long teamId, Integer number) {
        this.teamId = teamId;
        this.number = number;
    }
}
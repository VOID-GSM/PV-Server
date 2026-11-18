package pv.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private boolean approvalRequired;

    public Team(String name, boolean approvalRequired) {
        this.name = name;
        this.approvalRequired = approvalRequired;
    }

    public void changeApprovalRequired(boolean approvalRequired) {
        this.approvalRequired = approvalRequired;
    }
}
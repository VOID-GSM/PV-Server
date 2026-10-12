package pv.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long teamId;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id")
    private Generation generation;

    // 전공: 복수 선택
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_majors", joinColumns = @JoinColumn(name = "member_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "major")
    private Set<Major> majors = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    // 역할: 복수 보유 가능
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_roles", joinColumns = @JoinColumn(name = "member_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    private LocalDateTime createdAt;

    // 가입 시 기본 역할은 MEMBER
    public Member(Long teamId, String name, Generation generation, Set<Major> majors, MemberStatus status) {
        this.teamId = teamId;
        this.name = name;
        this.generation = generation;
        this.majors.addAll(majors);
        this.status = status;
        this.roles.add(Role.MEMBER);
        this.createdAt = LocalDateTime.now();
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    // 부장 = 가입 완료 상태 + LEADER 역할
    public boolean isLeader() {
        return status == MemberStatus.JOINED && roles.contains(Role.LEADER);
    }

    // 부장은 모든 권한을 가짐 (9기/스터디장 권한이 필요한 곳에서도 이걸로 체크)
    public boolean hasPermission(Role required) {
        return status == MemberStatus.JOINED
                && (roles.contains(Role.LEADER) || roles.contains(required));
    }

    public void approve() {
        this.status = MemberStatus.JOINED;
    }

    // 재입장: 부장은 승인 불필요, 나머지는 승인 기능이 켜져 있으면 다시 승인 대기
    public void reEnter(boolean approvalRequired) {
        if (roles.contains(Role.LEADER)) {
            this.status = MemberStatus.JOINED;
            return;
        }
        this.status = approvalRequired ? MemberStatus.PENDING : MemberStatus.JOINED;
    }

    // null이 아닌 값만 수정
    public void updateProfile(String name, Generation generation, Set<Major> majors) {
        if (name != null) this.name = name;
        if (generation != null) this.generation = generation;
        if (majors != null) {
            this.majors.clear();
            this.majors.addAll(majors);
        }
    }

    public void grantRole(Role role) {
        roles.add(role);
    }

    // 역할이 하나도 안 남으면 기본 MEMBER로 설정
    public void removeRole(Role role) {
        roles.remove(role);
        if (roles.isEmpty()) {
            roles.add(Role.MEMBER);
        }
    }
}
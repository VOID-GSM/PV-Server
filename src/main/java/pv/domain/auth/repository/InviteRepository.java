package pv.domain.auth.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.auth.entity.Invite;

import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, Long> {

    // 초대 링크 확인: GET /invites/{inviteCode}
    Optional<Invite> findByCode(String code);

    // 가입 시 사용 횟수 동시성 방지 (동시에 가입해도 maxUses 초과 방지)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invite i where i.code = :code")
    Optional<Invite> findWithLockByCode(@Param("code") String code);

    // 초대 링크 삭제 시 팀 소속 검증용: DELETE /teams/{teamId}/invites/{inviteId}
    Optional<Invite> findByIdAndTeamId(Long id, Long teamId);
}
package pv.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.auth.entity.RefreshToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByMemberIdAndExpiresAtBefore(Long memberId, LocalDateTime time);

    // 로그인된 기기 수: 기기마다 refresh 토큰이 하나씩이므로 만료되지 않은 토큰 수를 센다.
    long countByMemberIdAndExpiresAtAfter(Long memberId, LocalDateTime time);
}
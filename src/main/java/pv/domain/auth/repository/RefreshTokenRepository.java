package pv.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.auth.entity.RefreshToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByMemberIdAndExpiresAtBefore(Long memberId, LocalDateTime time);
}
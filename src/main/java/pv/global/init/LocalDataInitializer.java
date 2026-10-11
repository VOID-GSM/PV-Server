package pv.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.auth.entity.RefreshToken;
import pv.domain.auth.entity.Team;
import pv.domain.auth.repository.RefreshTokenRepository;
import pv.domain.auth.repository.TeamRepository;
import pv.domain.member.entity.Generation;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.entity.Role;
import pv.domain.member.repository.GenerationRepository;
import pv.domain.member.repository.MemberRepository;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

// local 프로필에서만 실행. DB가 비어 있을 때 팀/기수/부장을 만들고 부장의 refresh 토큰을 콘솔에 출력
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDataInitializer implements CommandLineRunner {

    private final TeamRepository teamRepository;
    private final GenerationRepository generationRepository;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (teamRepository.count() > 0) {
            log.info("[LocalDataInitializer] 이미 데이터가 있어서 건너뜁니다.");
            return;
        }

        Team team = teamRepository.save(new Team("테스트팀", true));
        Generation gen9 = generationRepository.save(new Generation(team.getId(), 9));
        generationRepository.save(new Generation(team.getId(), 10));

        Member leader = new Member(team.getId(), "부장", gen9, Set.of(Major.BACKEND), MemberStatus.JOINED);
        leader.grantRole(Role.LEADER);
        leader = memberRepository.save(leader);

        String token = UUID.randomUUID().toString().replace("-", "");
        refreshTokenRepository.save(new RefreshToken(leader.getId(), token, LocalDateTime.now().plusDays(30)));

        log.info("=================================================");
        log.info("[LocalDataInitializer] 팀 id={}, 부장 memberId={}", team.getId(), leader.getId());
        log.info("[LocalDataInitializer] 부장 refreshToken = {}", token);
        log.info("=================================================");
    }
}
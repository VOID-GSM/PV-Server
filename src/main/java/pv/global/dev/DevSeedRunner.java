package pv.global.dev;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.entity.Role;
import pv.global.security.JwtProvider;

import java.util.HashSet;

// 부장 토큰을 받기 위한 로컬용 파일입니다!!
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DevSeedRunner implements ApplicationRunner {

    private static final Long TEAM_ID = 1L;

    @PersistenceContext
    private EntityManager em;

    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Member leader = findOrCreate("테스트부장", true);
        Member member = findOrCreate("테스트부원", false);

        log.info("==== DEV SEED ====");
        log.info("부장 id={} teamId={}", leader.getId(), leader.getTeamId());
        log.info("부장 TOKEN: {}", jwtProvider.createAccessToken(leader.getId()));
        log.info("부원 id={} teamId={}", member.getId(), member.getTeamId());
        log.info("부원 TOKEN: {}", jwtProvider.createAccessToken(member.getId()));
        log.info("==================");
    }

    private Member findOrCreate(String name, boolean leader) {
        return em.createQuery("select m from Member m where m.name = :n", Member.class)
                .setParameter("n", name)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    Member m = new Member(TEAM_ID, name, null, new HashSet<>(), MemberStatus.JOINED);
                    if (leader) m.grantRole(Role.LEADER);
                    em.persist(m);
                    return m;
                });
    }
}
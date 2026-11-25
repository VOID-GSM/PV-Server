package pv.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findAllByTeamIdAndStatusOrderByCreatedAtAsc(Long teamId, MemberStatus status);

    @Query("""
            select m.id from Member m
            where m.teamId = :teamId
              and m.status = pv.domain.member.entity.MemberStatus.JOINED
              and m.id in :ids
            """)
    List<Long> findJoinedIds(Long teamId, Collection<Long> ids);

    @Query("""
            select m.id from Member m
            where m.teamId = :teamId
              and m.status = pv.domain.member.entity.MemberStatus.JOINED
              and m.generation.number < :gen
            """)
    List<Long> findSeniorIds(Long teamId, int gen);

    @Query("""
            select distinct m.id from Member m join m.majors mj
            where m.teamId = :teamId
              and m.status = pv.domain.member.entity.MemberStatus.JOINED
              and m.generation.number < :gen
              and mj in :majors
            """)
    List<Long> findSeniorIdsByMajors(Long teamId, int gen, Collection<Major> majors);

    // ===== 공지/알림용 (아래 3개 추가) =====

    // 팀의 가입 완료 멤버 ID 전체 (공지 ALL/BOARD 대상자)
    @Query("""
            select m.id from Member m
            where m.teamId = :teamId
              and m.status = pv.domain.member.entity.MemberStatus.JOINED
            """)
    List<Long> findJoinedIdsByTeamId(Long teamId);

    // 지정한 기수에 속하고 지정한 전공 중 하나라도 가진 가입 완료 멤버 ID (공지 TARGET 대상자)
    @Query("""
            select distinct m.id from Member m join m.majors mj
            where m.teamId = :teamId
              and m.status = pv.domain.member.entity.MemberStatus.JOINED
              and m.generation.id in :generationIds
              and mj in :majors
            """)
    List<Long> findJoinedIdsByGenerationsAndMajors(Long teamId,
                                                   Collection<Long> generationIds,
                                                   Collection<Major> majors);

    // 이름만 필요할 때 Member(roles/majors EAGER 컬렉션)를 통째로 로딩하지 않기 위함
    @Query("select m.name from Member m where m.id = :id")
    Optional<String> findNameById(Long id);

    // 여러 명의 이름을 한 번에 조회 (roles/majors EAGER 컬렉션 로딩 없이 쿼리 1번)
    @Query("select m.id as id, m.name as name from Member m where m.id in :ids")
    List<IdName> findNamesByIds(@Param("ids") Collection<Long> ids);

    interface IdName {
        Long getId();
        String getName();
    }

    // 과제 대상자 계산 (ALL): 같은 팀, 가입 완료, 현재 활동 기수 이상, 출제자·선배·부장 제외
    // currentGen이 null이면(팀에 기수가 없으면) 기수 조건 없이 계산
    @Query("""

            select m.id from Member m left join m.generation g
        where m.teamId = :teamId
          and m.status = pv.domain.member.entity.MemberStatus.JOINED
          and m.id <> :authorId
          and (:currentGen is null or g.number >= :currentGen)
          and not exists (select 1 from Member x join x.roles r
                          where x.id = m.id
                            and r in (pv.domain.member.entity.Role.SENIOR,
                                      pv.domain.member.entity.Role.LEADER))
        """)
    List<Long> findAssignmentBaseIds(@Param("teamId") Long teamId,
                                     @Param("authorId") Long authorId,
                                     @Param("currentGen") Integer currentGen);

    @Query("""
        select distinct m.id from Member m join m.majors mj left join m.generation g
        where m.teamId = :teamId
          and m.status = pv.domain.member.entity.MemberStatus.JOINED
          and m.id <> :authorId
          and mj in :majors
          and (:currentGen is null or g.number >= :currentGen)
          and not exists (select 1 from Member x join x.roles r
                          where x.id = m.id
                            and r in (pv.domain.member.entity.Role.SENIOR,
                                      pv.domain.member.entity.Role.LEADER))
        """)
    List<Long> findAssignmentMajorIds(@Param("teamId") Long teamId,
                                      @Param("authorId") Long authorId,
                                      @Param("majors") Collection<Major> majors,
                                      @Param("currentGen") Integer currentGen);
    }
package pv.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pv.domain.auth.entity.ReEntryLink;

import java.util.Optional;

public interface ReEntryLinkRepository extends JpaRepository<ReEntryLink, Long> {

    Optional<ReEntryLink> findByCode(String code);

    Optional<ReEntryLink> findByMemberId(Long memberId);
}
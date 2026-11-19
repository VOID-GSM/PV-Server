package pv.domain.member.entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MemberReEntryTest {

    private Member member(MemberStatus status) {
        return new Member(1L, "테스트", null, Set.of(), status);
    }

    @Test
    void 승인된_멤버는_승인기능이_켜져도_JOINED를_유지한다() {
        Member m = member(MemberStatus.JOINED);
        m.reEnter(true);
        assertThat(m.getStatus()).isEqualTo(MemberStatus.JOINED);
    }

    @Test
    void 승인_대기_멤버는_승인기능이_켜져_있으면_PENDING을_유지한다() {
        Member m = member(MemberStatus.PENDING);
        m.reEnter(true);
        assertThat(m.getStatus()).isEqualTo(MemberStatus.PENDING);
    }

    @Test
    void 승인_대기_멤버는_승인기능이_꺼져_있으면_JOINED가_된다() {
        Member m = member(MemberStatus.PENDING);
        m.reEnter(false);
        assertThat(m.getStatus()).isEqualTo(MemberStatus.JOINED);
    }

    @Test
    void 부장은_승인기능과_상관없이_JOINED가_된다() {
        Member m = member(MemberStatus.PENDING);
        m.grantRole(Role.LEADER);
        m.reEnter(true);
        assertThat(m.getStatus()).isEqualTo(MemberStatus.JOINED);
    }
}
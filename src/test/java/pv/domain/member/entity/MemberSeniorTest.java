package pv.domain.member.entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MemberSeniorTest {

    private Member member(Integer generationNumber, MemberStatus status) {
        Generation generation = generationNumber == null ? null : new Generation(1L, generationNumber);
        return new Member(1L, "테스트", generation, Set.of(), status);
    }

    @Test
    void 현재_기수보다_낮은_기수는_선배다() {
        assertThat(member(9, MemberStatus.JOINED).isSeniorOf(10)).isTrue();
    }

    @Test
    void 현재_기수와_같은_기수는_선배가_아니다() {
        assertThat(member(10, MemberStatus.JOINED).isSeniorOf(10)).isFalse();
    }

    @Test
    void 새_기수가_생기면_기존_기수도_선배가_된다() {
        assertThat(member(10, MemberStatus.JOINED).isSeniorOf(11)).isTrue();
    }

    @Test
    void 기수가_없거나_가입_대기면_선배가_아니다() {
        assertThat(member(null, MemberStatus.JOINED).isSeniorOf(10)).isFalse();
        assertThat(member(9, MemberStatus.PENDING).isSeniorOf(10)).isFalse();
    }
}

package pv.domain.interviews.service;

import org.springframework.web.server.ResponseStatusException;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.Role;

import static org.springframework.http.HttpStatus.FORBIDDEN;

final class InterviewAuthorizer {
    private InterviewAuthorizer() {}

    static void checkOwnerOrAdmin(Member owner, Member me) {
        if (!owner.getId().equals(me.getId()) && !me.isLeader())
            throw new ResponseStatusException(FORBIDDEN, "만든 사람 또는 부장만 가능합니다.");
    }
}

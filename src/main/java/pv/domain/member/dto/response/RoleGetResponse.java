package pv.domain.member.dto.response;

import pv.domain.member.entity.Role;

import java.util.Set;

public record RoleGetResponse(Long memberId, Set<Role> roles) {}
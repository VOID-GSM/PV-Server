package pv.domain.member.dto.request;

import jakarta.validation.constraints.Size;
import pv.domain.member.entity.Major;

import java.util.Set;

// 부장이 수정: 이름, 기수, 전공 (보낸 필드만 수정)
public record MemberUpdateRequest(String name, Long generationId, @Size(min = 1) Set<Major> majors) {}
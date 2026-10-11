package pv.domain.member.dto.request;

import jakarta.validation.constraints.Size;
import pv.domain.member.entity.Major;

import java.util.Set;

// 보낸 필드만 수정 (null은 변경 안 함). 기수는 부장만 변경 가능
public record MyProfileUpdateRequest(String name, @Size(min = 1) Set<Major> majors) {}
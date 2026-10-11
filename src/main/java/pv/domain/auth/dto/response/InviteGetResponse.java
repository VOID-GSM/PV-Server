package pv.domain.auth.dto.response;

import pv.domain.member.entity.Major;

import java.util.List;

// 프론트가 프로필 입력 화면(기수 선택, 전공 선택)을 그리는 데 필요한 정보 포함
public record InviteGetResponse(
        Long teamId, String teamName, boolean approvalRequired, String code,
        List<GenerationItem> generations, List<Major> majors) {

    public record GenerationItem(Long id, Integer number) {}
}
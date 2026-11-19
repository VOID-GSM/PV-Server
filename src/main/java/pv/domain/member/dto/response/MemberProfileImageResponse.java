package pv.domain.member.dto.response;

import pv.domain.files.dto.FileDownloadResponse;

public record MemberProfileImageResponse(Long memberId, FileDownloadResponse image) {
}
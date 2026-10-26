package pv.domain.assignment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.assignment.dto.projection.*;
import pv.domain.assignment.dto.request.*;
import pv.domain.assignment.dto.response.*;
import pv.domain.assignment.entity.Assignment;
import pv.domain.assignment.entity.Submission;
import pv.domain.assignment.entity.SubmissionStatus;
import pv.domain.assignment.repository.AssignmentMemberQueryRepository;
import pv.domain.assignment.repository.AssignmentRepository;
import pv.domain.assignment.repository.SubmissionRepository;
import pv.domain.member.entity.Member;
import pv.domain.member.repository.MemberRepository;
import pv.global.exception.CustomException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentMemberQueryRepository memberQueryRepository;
    private final MemberRepository memberRepository;
    private final AttachmentValidator attachmentValidator;
    private final Clock clock;

    // 6. 제출 현황: 출제자 / 부장 — 쿼리 3회 고정 (과제, 대상자, 제출 목록)
    public SubmissionStatusResponse getStatus(Long memberId, Long assignmentId) {
        Member me = joinedMember(memberId);
        Assignment a = assignmentRepository.findDetail(assignmentId)
                .filter(x -> x.getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "과제를 찾을 수 없습니다."));
        if (!(a.isAuthor(me.getId()) || me.isLeader())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "출제자 또는 부장만 제출 현황을 볼 수 있습니다.");
        }

        Long teamId = a.getTeamId();
        Long authorId = a.getAuthor().getId();
        List<TargetMemberRow> targets = switch (a.getTargetType()) {
            case ALL -> memberQueryRepository.findAllJoined(teamId, authorId);
            case MAJOR -> memberQueryRepository.findJoinedByMajors(teamId, authorId, a.getTargetMajors());
            case MEMBER -> memberQueryRepository.findJoinedByIds(teamId, authorId, a.getTargetMemberIds());
        };

        Map<Long, SubmissionRow> rows = submissionRepository.findRows(assignmentId).stream()
                .collect(Collectors.toMap(SubmissionRow::submitterId, Function.identity()));

        List<SubmissionStatusResponse.Item> items = targets.stream()
                .map(t -> SubmissionStatusResponse.Item.of(t, rows.get(t.id())))
                .toList();
        return SubmissionStatusResponse.of(assignmentId, items);
    }

    // 7. 제출: 대상자만. 마감 판정은 과제 공유 락 획득 이후 시각 기준
    // 7. 제출: 대상자만. 마감 판정은 과제 공유 락 획득 이후 시각 기준
    @Transactional
    public SubmissionResponse submit(Long memberId, Long assignmentId, SubmissionRequest req) {
        Member me = joinedMember(memberId);
        requireAnyContent(req);
        List<String> fileKeys = attachmentValidator.normalize(req.fileKeys(), memberId);

        Assignment a = assignmentRepository.findByIdForShare(assignmentId)
                .filter(x -> x.getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "과제를 찾을 수 없습니다."));
        if (!a.isTarget(me)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "과제 대상자만 제출할 수 있습니다.");
        }
        if (submissionRepository.existsByAssignmentIdAndSubmitterId(assignmentId, me.getId())) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 제출한 과제입니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock); // 락 획득 후 시각
        try {
            Submission saved = submissionRepository.saveAndFlush(new Submission(
                    a, me, blankToNull(req.content()), fileKeys, blankToNull(req.githubUrl()), now));
            return SubmissionResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 제출한 과제입니다.");
        }
    }
    // 8. 제출물 상세: 제출자 / 출제자 / 부장
    public SubmissionResponse get(Long memberId, Long assignmentId, Long submissionId) {
        Member me = joinedMember(memberId);
        Submission s = submissionRepository.findDetail(submissionId)
                .filter(x -> x.belongsTo(assignmentId) && x.getAssignment().getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "제출물을 찾을 수 없습니다."));
        if (!(s.isSubmittedBy(me.getId()) || s.getAssignment().isAuthor(me.getId()) || me.isLeader())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "조회 권한이 없습니다.");
        }
        return SubmissionResponse.from(s);
    }

    // 9. 재제출: 제출자 본인 + 재제출 요청 상태만
    @Transactional
    public void resubmit(Long memberId, Long assignmentId, Long submissionId, SubmissionRequest req) {
        Member me = joinedMember(memberId);
        requireAnyContent(req);
        List<String> fileKeys = attachmentValidator.normalize(req.fileKeys(), memberId);

        Submission s = lockedSubmission(assignmentId, submissionId);
        if (!s.isSubmittedBy(me.getId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "본인 제출물만 수정할 수 있습니다.");
        }
        if (s.getStatus() != SubmissionStatus.RESUBMIT_REQUESTED) {
            throw new CustomException(HttpStatus.CONFLICT, "재제출이 요청된 제출물만 수정할 수 있습니다.");
        }
        s.resubmit(blankToNull(req.content()), fileKeys, blankToNull(req.githubUrl()), LocalDateTime.now(clock));
    }

    // 10. 통과: 출제자만. 상태 전이만 수행 (역량 실적은 PASSED 제출물 집계로 읽음)
    // Submission 행 락 + SUBMITTED일 때만 전이 → 같은 제출물의 중복 통과 불가
    @Transactional
    public void pass(Long memberId, Long assignmentId, Long submissionId, ReviewRequest req) {
        Member me = joinedMember(memberId);
        Submission s = lockedSubmission(assignmentId, submissionId);
        authorOf(me, s);
        if (s.getStatus() != SubmissionStatus.SUBMITTED) {
            throw new CustomException(HttpStatus.CONFLICT, "제출 완료 상태의 제출물만 통과 처리할 수 있습니다.");
        }
        s.pass(req == null ? null : blankToNull(req.comment()), LocalDateTime.now(clock));
    }

    // 11. 재제출 요청: 출제자만, 코멘트 필수
    @Transactional
    public void requestResubmission(Long memberId, Long assignmentId, Long submissionId, ResubmissionRequest req) {
        Member me = joinedMember(memberId);
        Submission s = lockedSubmission(assignmentId, submissionId);
        authorOf(me, s);
        if (s.getStatus() != SubmissionStatus.SUBMITTED) {
            throw new CustomException(HttpStatus.CONFLICT, "제출 완료 상태의 제출물만 재제출을 요청할 수 있습니다.");
        }
        s.requestResubmission(req.comment().strip(), LocalDateTime.now(clock));
    }

    private Submission lockedSubmission(Long assignmentId, Long submissionId) {
        return submissionRepository.findByIdForUpdate(submissionId)
                .filter(x -> x.belongsTo(assignmentId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "제출물을 찾을 수 없습니다."));
    }

    private Assignment authorOf(Member me, Submission s) {
        Assignment a = s.getAssignment();
        if (!a.getTeamId().equals(me.getTeamId()) || !a.isAuthor(me.getId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "출제자만 처리할 수 있습니다.");
        }
        return a;
    }

    private void requireAnyContent(SubmissionRequest req) {
        boolean hasText = req.content() != null && !req.content().isBlank();
        boolean hasFiles = req.fileKeys() != null && !req.fileKeys().isEmpty();
        boolean hasGithub = req.githubUrl() != null && !req.githubUrl().isBlank();
        if (!(hasText || hasFiles || hasGithub)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "텍스트, 파일, GitHub 링크 중 하나 이상을 제출해 주세요.");
        }
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    private Member joinedMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        if (!member.isJoined()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "승인된 팀원만 이용할 수 있습니다.");
        }
        return member;
    }
}
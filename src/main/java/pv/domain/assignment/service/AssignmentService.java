package pv.domain.assignment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.assignment.dto.projection.*;
import pv.domain.assignment.dto.request.*;
import pv.domain.assignment.dto.response.*;
import pv.domain.assignment.entity.Assignment;
import pv.domain.assignment.entity.AssignmentTargetType;
import pv.domain.assignment.repository.AssignmentRepository;
import pv.domain.assignment.repository.CompetencyRecordRepository;
import pv.domain.assignment.repository.SubmissionRepository;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.Role;
import pv.domain.member.repository.MemberRepository;
import pv.global.exception.CustomException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CompetencyRecordRepository competencyRecordRepository;
    private final MemberRepository memberRepository;
    private final AttachmentValidator attachmentValidator;

    // 1. 목록: 쿼리 2~3회 고정 (페이지 + 내 제출 상태 or 제출 수)
    public PageResponse<AssignmentSummaryResponse> list(Long memberId, AssignmentListType type, Pageable pageable) {
        Member me = joinedMember(memberId);
        boolean received = type == AssignmentListType.RECEIVED;

        Page<Assignment> page = received
                ? assignmentRepository.findReceived(me.getTeamId(), me.getId(), me.getMajors(), pageable)
                : assignmentRepository.findCreated(me.getId(), pageable);

        List<Long> ids = page.getContent().stream().map(Assignment::getId).toList();
        Map<Long, MySubmissionRow> mine = Map.of();
        Map<Long, Long> counts = Map.of();
        if (!ids.isEmpty()) {
            if (received) {
                mine = submissionRepository.findMine(me.getId(), ids).stream()
                        .collect(Collectors.toMap(MySubmissionRow::assignmentId, Function.identity()));
            } else {
                counts = submissionRepository.countByAssignmentIds(ids).stream()
                        .collect(Collectors.toMap(SubmissionCountRow::assignmentId, SubmissionCountRow::count));
            }
        }

        Map<Long, MySubmissionRow> mineMap = mine;
        Map<Long, Long> countMap = counts;
        return PageResponse.from(page.map(a -> AssignmentSummaryResponse.of(
                a, mineMap.get(a.getId()), received ? null : countMap.getOrDefault(a.getId(), 0L))));
    }

    // 2. 출제: 부장(LEADER) 또는 선배(SENIOR)
    @Transactional
    public Long create(Long memberId, AssignmentCreateRequest req) {
        Member me = joinedMember(memberId);
        if (!me.hasPermission(Role.SENIOR)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "선배 기수 또는 부장만 과제를 출제할 수 있습니다.");
        }
        if (!req.deadline().isAfter(LocalDateTime.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "마감 시간은 현재 이후여야 합니다.");
        }

        Set<Major> majors = req.targetMajors() == null ? Set.of() : req.targetMajors();
        Set<Long> memberIds = req.targetMemberIds() == null ? Set.of() : req.targetMemberIds();
        switch (req.targetType()) {
            case ALL -> {
                majors = Set.of();
                memberIds = Set.of();
            }
            case MAJOR -> {
                if (majors.isEmpty()) {
                    throw new CustomException(HttpStatus.BAD_REQUEST, "대상 전공을 선택해 주세요.");
                }
                memberIds = Set.of();
            }
            case MEMBER -> {
                if (memberIds.isEmpty()) {
                    throw new CustomException(HttpStatus.BAD_REQUEST, "대상 팀원을 선택해 주세요.");
                }
                if (memberRepository.findJoinedIds(me.getTeamId(), memberIds).size() != memberIds.size()) {
                    throw new CustomException(HttpStatus.BAD_REQUEST, "팀에 가입된 팀원만 대상으로 지정할 수 있습니다.");
                }
                majors = Set.of();
            }
        }

        List<String> fileKeys = attachmentValidator.normalize(req.fileKeys());
        List<String> competencies = req.competencies() == null ? List.of()
                : req.competencies().stream().map(String::strip).distinct().toList();

        return assignmentRepository.save(new Assignment(
                me.getTeamId(), me, req.title().strip(), req.description().strip(), req.deadline(),
                req.targetType(), majors, memberIds, fileKeys, competencies)).getId();
    }

    // 3. 상세: 출제자 / 부장 / 대상자
    public AssignmentDetailResponse get(Long memberId, Long assignmentId) {
        Member me = joinedMember(memberId);
        Assignment a = assignmentRepository.findDetail(assignmentId)
                .filter(x -> x.getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "과제를 찾을 수 없습니다."));
        if (!(a.isAuthor(me.getId()) || me.isLeader() || a.isTarget(me))) {
            throw new CustomException(HttpStatus.FORBIDDEN, "조회 권한이 없습니다.");
        }
        return AssignmentDetailResponse.from(a);
    }

    // 4. 수정: 출제자 본인만
    @Transactional
    public void update(Long memberId, Long assignmentId, AssignmentUpdateRequest req) {
        Member me = joinedMember(memberId);
        Assignment a = assignmentRepository.findByIdForUpdate(assignmentId)
                .filter(x -> x.getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "과제를 찾을 수 없습니다."));
        if (!a.isAuthor(me.getId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "출제자만 과제를 수정할 수 있습니다.");
        }
        if (req.deadline() != null && !req.deadline().isAfter(LocalDateTime.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "마감 시간은 현재 이후여야 합니다.");
        }
        List<String> fileKeys = req.fileKeys() == null ? null : attachmentValidator.normalize(req.fileKeys());
        List<String> competencies = req.competencies() == null ? null
                : req.competencies().stream().map(String::strip).distinct().toList();

        a.update(req.title() == null ? null : req.title().strip(),
                req.description() == null ? null : req.description().strip(),
                req.deadline(), fileKeys, competencies);
    }

    // 5. 삭제: 출제자 또는 부장 (제출물·역량 기록 함께 삭제)
    @Transactional
    public void delete(Long memberId, Long assignmentId) {
        Member me = joinedMember(memberId);
        Assignment a = assignmentRepository.findByIdForUpdate(assignmentId)
                .filter(x -> x.getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "과제를 찾을 수 없습니다."));
        if (!(a.isAuthor(me.getId()) || me.isLeader())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "출제자 또는 부장만 삭제할 수 있습니다.");
        }
        competencyRecordRepository.deleteAllByAssignmentId(assignmentId);
        submissionRepository.deleteAllByAssignmentId(assignmentId);
        assignmentRepository.delete(a);
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
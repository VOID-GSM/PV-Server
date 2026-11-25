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
import pv.domain.assignment.entity.Submission;
import org.springframework.context.ApplicationEventPublisher;
import pv.domain.assignment.repository.AssignmentRecipientRepository;
import pv.domain.assignment.repository.AssignmentRepository;
import pv.domain.assignment.repository.SubmissionRepository;
import pv.domain.files.service.FileService;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.Role;
import pv.domain.member.repository.GenerationRepository;
import pv.domain.member.repository.MemberRepository;
import pv.domain.notification.entity.NotificationType;
import pv.domain.notification.event.DiscordNoticeEvent;
import pv.domain.notification.message.DiscordMessageFormatter;
import pv.domain.notification.service.NotificationService;
import pv.domain.skills.entity.Skill;
import pv.domain.skills.repository.SkillRepository;
import pv.global.exception.CustomException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final AssignmentRecipientRepository recipientRepository;
    private final SubmissionRepository submissionRepository;
    private final MemberRepository memberRepository;
    private final GenerationRepository generationRepository;
    private final SkillRepository skillRepository;
    private final AttachmentValidator attachmentValidator;
    private final FileService fileService;
    private final Clock clock;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    // 같은 과제에 대한 미제출 알림 최소 간격 (알림 연타 방지)
    private static final Duration REMIND_INTERVAL = Duration.ofHours(1);

    // 1. 목록
    public PageResponse<AssignmentSummaryResponse> list(
            Long memberId,
            AssignmentListType type,
            Pageable pageable
    ) {
        Member me = joinedMember(memberId);
        boolean received = type == AssignmentListType.RECEIVED;

        Page<Assignment> page = received
                ? assignmentRepository.findReceived(me.getId(), pageable)
                : assignmentRepository.findCreated(me.getId(), pageable);

        List<Long> ids = page.getContent()
                .stream()
                .map(Assignment::getId)
                .toList();

        Map<Long, MySubmissionRow> mine = Map.of();
        Map<Long, Long> counts = Map.of();

        if (!ids.isEmpty()) {
            if (received) {
                mine = submissionRepository.findMine(me.getId(), ids)
                        .stream()
                        .collect(Collectors.toMap(
                                MySubmissionRow::assignmentId,
                                Function.identity()
                        ));
            } else {
                counts = submissionRepository.countByAssignmentIds(ids)
                        .stream()
                        .collect(Collectors.toMap(
                                SubmissionCountRow::assignmentId,
                                SubmissionCountRow::count
                        ));
            }
        }

        Map<Long, MySubmissionRow> mineMap = mine;
        Map<Long, Long> countMap = counts;

        return PageResponse.from(page.map(a ->
                AssignmentSummaryResponse.of(
                        a,
                        mineMap.get(a.getId()),
                        received ? null : countMap.getOrDefault(a.getId(), 0L)
                )
        ));
    }

    // 2. 출제: 선배 기수 또는 부장
    @Transactional
    public Long create(Long memberId, AssignmentCreateRequest req) {
        Member me = joinedMember(memberId);

        // 현재 활동 기수 (팀의 최신 기수). 새 기수가 추가되면 자동으로 바뀐다.
        Integer currentGen = generationRepository.findCurrentNumberByTeamId(me.getTeamId()).orElse(null);

        if (!canCreateAssignment(me, currentGen)) {
            throw new CustomException(
                    HttpStatus.FORBIDDEN,
                    "선배 기수 또는 부장만 과제를 출제할 수 있습니다."
            );
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (!req.deadline().isAfter(now)) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "마감 시간은 현재 이후여야 합니다."
            );
        }

        Set<Major> majors = req.targetMajors() == null
                ? Set.of()
                : req.targetMajors();

        Set<Long> memberIds = req.targetMemberIds() == null
                ? Set.of()
                : req.targetMemberIds();

        switch (req.targetType()) {
            case ALL -> {
                majors = Set.of();
                memberIds = Set.of();
            }
            case MAJOR -> {
                if (majors.isEmpty()) {
                    throw new CustomException(
                            HttpStatus.BAD_REQUEST,
                            "대상 전공을 선택해 주세요."
                    );
                }
                memberIds = Set.of();
            }
            case MEMBER -> {
                if (memberIds.isEmpty()) {
                    throw new CustomException(
                            HttpStatus.BAD_REQUEST,
                            "대상 팀원을 선택해 주세요."
                    );
                }

                if (memberRepository.findJoinedIds(
                        me.getTeamId(),
                        memberIds
                ).size() != memberIds.size()) {
                    throw new CustomException(
                            HttpStatus.BAD_REQUEST,
                            "팀에 가입된 팀원만 대상으로 지정할 수 있습니다."
                    );
                }

                majors = Set.of();
            }
        }

        List<String> fileKeys =
                attachmentValidator.attachNew(memberId, req.fileKeys());

        Set<Long> skillIds = validatedSkillIds(req.skillIds());

        Assignment saved = assignmentRepository.save(new Assignment(
                me.getTeamId(),
                me,
                req.title().strip(),
                req.description().strip(),
                req.deadline(),
                req.targetType(),
                majors,
                memberIds,
                fileKeys,
                skillIds,
                now
        ));

        List<Long> recipientIds = switch (req.targetType()) {
            case ALL -> memberRepository.findAssignmentBaseIds(me.getTeamId(), me.getId(), currentGen);
            case MAJOR -> memberRepository.findAssignmentMajorIds(me.getTeamId(), me.getId(), majors, currentGen);
            case MEMBER -> memberIds.stream()
                    .filter(id -> !id.equals(me.getId()))
                    .toList();
        };

        for (int i = 0; i < recipientIds.size(); i += 1000) {
            recipientRepository.insertAll(
                    saved.getId(),
                    recipientIds.subList(i, Math.min(i + 1000, recipientIds.size())));
        }

        return saved.getId();
    }

    // 3. 상세
    public AssignmentDetailResponse get(Long memberId, Long assignmentId) {
        Member me = joinedMember(memberId);

        Assignment a = assignmentRepository.findDetail(assignmentId)
                .filter(x -> x.getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "과제를 찾을 수 없습니다."
                ));

        if (!(a.isAuthor(me.getId()) || me.isLeader()
                || recipientRepository.existsByAssignmentIdAndMemberId(a.getId(), me.getId()))) {
            throw new CustomException(
                    HttpStatus.FORBIDDEN,
                    "조회 권한이 없습니다."
            );
        }

        Set<Long> skillIds = a.getSkillIds();

        List<Skill> skills = skillIds.isEmpty()
                ? List.of()
                : skillRepository.findAllById(skillIds)
                .stream()
                .sorted(
                        Comparator.comparing(Skill::getMajor)
                                .thenComparingInt(Skill::getSortOrder)
                                .thenComparing(Skill::getId)
                )
                .toList();

        return AssignmentDetailResponse.of(a, skills);
    }

    // 4. 수정: 출제자 본인만
    @Transactional
    public void update(
            Long memberId,
            Long assignmentId,
            AssignmentUpdateRequest req
    ) {
        Member me = joinedMember(memberId);

        Assignment a = assignmentRepository.findByIdForUpdate(assignmentId)
                .filter(x -> x.getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "과제를 찾을 수 없습니다."
                ));

        if (!a.isAuthor(me.getId())) {
            throw new CustomException(
                    HttpStatus.FORBIDDEN,
                    "출제자만 과제를 수정할 수 있습니다."
            );
        }

        if (req.deadline() != null
                && !req.deadline().isAfter(LocalDateTime.now(clock))) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "마감 시간은 현재 이후여야 합니다."
            );
        }

        List<String> fileKeys = req.fileKeys() == null
                ? null
                : attachmentValidator.replace(memberId, a.getFileKeys(), req.fileKeys());

        Set<Long> skillIds = req.skillIds() == null
                ? null
                : validatedSkillIds(req.skillIds());

        a.update(
                req.title() == null ? null : req.title().strip(),
                req.description() == null ? null : req.description().strip(),
                req.deadline(),
                fileKeys,
                skillIds
        );
    }

    // 5. 삭제: 출제자 또는 부장
    @Transactional
    public void delete(Long memberId, Long assignmentId) {
        Member me = joinedMember(memberId);

        Assignment a = assignmentRepository.findByIdForUpdate(assignmentId)
                .filter(x -> x.getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "과제를 찾을 수 없습니다."
                ));

        if (!(a.isAuthor(me.getId()) || me.isLeader())) {
            throw new CustomException(
                    HttpStatus.FORBIDDEN,
                    "출제자 또는 부장만 삭제할 수 있습니다."
            );
        }

        // 과제 및 제출물에 연결된 파일 키 수집
        List<String> fileKeys = new ArrayList<>(a.getFileKeys());

        List<Submission> submissions =
                submissionRepository.findAllByAssignmentId(assignmentId);

        for (Submission submission : submissions) {
            if (submission.getFileKeys() != null) {
                fileKeys.addAll(submission.getFileKeys());
            }
        }

        // DB에서 제출물과 과제 삭제 (미제출 알림도 함께 정리)
        notificationService.deleteByTarget(NotificationType.ASSIGNMENT_REMINDER, assignmentId);
        recipientRepository.deleteAllByAssignmentId(assignmentId);
        submissionRepository.deleteAllByAssignmentId(assignmentId);
        assignmentRepository.delete(a);

        // FileService가 DB 레코드와 S3 객체 정리를 처리
        fileService.deleteByKeys(fileKeys);
    }

    // 6. 미제출 알림: 출제자 또는 부장. 대상자 중 아직 제출하지 않은 사람에게만 보낸다.
    // 과제 행을 잠가서 동시에 여러 번 눌러도 간격 제한이 정확히 적용된다.
    @Transactional
    public AssignmentRemindResponse remind(Long memberId, Long assignmentId, boolean discordEnabled) {
        Member me = joinedMember(memberId);

        Assignment a = assignmentRepository.findByIdForUpdate(assignmentId)
                .filter(x -> x.getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "과제를 찾을 수 없습니다."
                ));

        if (!(a.isAuthor(me.getId()) || me.isLeader())) {
            throw new CustomException(
                    HttpStatus.FORBIDDEN,
                    "출제자 또는 부장만 미제출 알림을 보낼 수 있습니다."
            );
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Duration remaining = a.remainingRemindCooldown(now, REMIND_INTERVAL);
        if (!remaining.isZero()) {
            long minutes = remaining.toMinutes() + 1;
            throw new CustomException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "미제출 알림은 1시간에 한 번만 보낼 수 있습니다. 약 " + minutes + "분 후에 다시 시도해주세요."
            );
        }

        List<Long> unsubmittedIds = recipientRepository.findUnsubmittedMemberIds(assignmentId);
        if (unsubmittedIds.isEmpty()) {
            return new AssignmentRemindResponse(0);
        }

        a.markReminded(now);
        int reminded = notificationService.send(
                NotificationType.ASSIGNMENT_REMINDER,
                "[과제 미제출] " + a.getTitle(),
                assignmentId,
                unsubmittedIds
        );

        // 디스코드: 선택한 경우에만, 디스코드 ID가 등록된 미제출자가 있을 때만 멘션해서 보낸다.
        if (discordEnabled) {
            List<String> discordIds = memberRepository.findDiscordIds(unsubmittedIds);
            if (!discordIds.isEmpty()) {
                eventPublisher.publishEvent(new DiscordNoticeEvent(
                        a.getTeamId(),
                        DiscordMessageFormatter.compose(
                                DiscordMessageFormatter.userMentions(discordIds),
                                a.getTitle() + " 과제를 완료해주세요",
                                ""),
                        false, Set.of(), Set.copyOf(discordIds)
                ));
            }
        }

        return new AssignmentRemindResponse(reminded);
    }

    // 존재하고 활성인 스킬만 허용
    private Set<Long> validatedSkillIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }

        if (skillRepository.findAllByIdInAndActiveTrue(ids).size() != ids.size()) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "존재하지 않거나 비활성화된 스킬이 포함되어 있습니다."
            );
        }

        return Set.copyOf(ids);
    }

    // 출제 권한: 부장, 기존 SENIOR 역할, 또는 현재 활동 기수보다 낮은 기수(선배)
    private boolean canCreateAssignment(Member me, Integer currentGen) {
        if (me.hasPermission(Role.SENIOR)) {
            return true;
        }
        return currentGen != null && me.isSeniorOf(currentGen);
    }

    private Member joinedMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "회원을 찾을 수 없습니다."
                ));

        if (!member.isJoined()) {
            throw new CustomException(
                    HttpStatus.FORBIDDEN,
                    "승인된 팀원만 이용할 수 있습니다."
            );
        }

        return member;
    }
}
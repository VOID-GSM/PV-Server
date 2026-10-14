package pv.domain.activity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.activity.dto.request.*;
import pv.domain.activity.dto.response.*;
import pv.domain.activity.entity.*;
import pv.domain.activity.repository.*;
import pv.domain.member.entity.Generation;
import pv.domain.activity.dto.query.*;
import pv.domain.member.entity.Major;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.repository.GenerationRepository;
import pv.domain.member.repository.MemberRepository;
import pv.global.exception.CustomException;
import java.time.Clock;
import java.time.ZoneId;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private final Clock clock;

    // 반복 회차를 자동 생성하는 최대 기간 (회차 폭증 방지)
    private static final long MAX_PERIOD_DAYS = 366;

    // 기본 주제 투표 오픈 일수
    private static final int DEFAULT_TOPIC_OPEN_DAYS = 3;

    // 투표 마감 시간
    private static final int TOPIC_VOTE_DEADLINE_HOUR = 15;

    private final ActivityRepository activityRepository;
    private final ActivityMemberRepository activityMemberRepository;
    private final ActivityApplicationRepository applicationRepository;
    private final ActivitySessionRepository sessionRepository;
    private final TopicCandidateRepository candidateRepository;
    private final TopicVoteRepository voteRepository;
    private final MemberRepository memberRepository;
    private final GenerationRepository generationRepository;

    // ===== 활동 =====

    // 필터는 팀 활동 수가 적다는 가정으로 메모리에서 처리
    // 컬렉션은 @BatchSize로 IN 조회
    public List<ActivitySummaryResponse> getActivities(
            Long loginMemberId,
            ActivityType type,
            Major major,
            Long generationId,
            ActivityStatus status
    ) {
        Member member = getMember(loginMemberId);

        List<Activity> activities =
                activityRepository.findAllByTeamIdOrderByIdDesc(member.getTeamId())
                        .stream()
                        .filter(a -> type == null || a.getType() == type)
                        .filter(a -> status == null || a.getStatus() == status)
                        .filter(a -> major == null
                                || a.getTargetMajors().isEmpty()
                                || a.getTargetMajors().contains(major))
                        .filter(a -> generationId == null
                                || a.getTargetGenerationIds().isEmpty()
                                || a.getTargetGenerationIds().contains(generationId))
                        .toList();

        return toSummaries(activities);
    }

    // 개설자는 자동으로 스터디장으로 등록
    // 활동 기간 안의 반복 회차를 한 번에 생성
    @Transactional
    public ActivitySummaryResponse createActivity(
            Long loginMemberId,
            ActivityCreateRequest request
    ) {
        Member member = getMember(loginMemberId);

        validatePeriod(request.startDate(), request.endDate());

        Set<Long> generationIds =
                request.targetGenerationIds() == null
                        ? Set.of()
                        : request.targetGenerationIds();

        validateGenerations(member, generationIds);

        DayOfWeek day = request.repeatDayOfWeek() == null
                ? DayOfWeek.WEDNESDAY
                : request.repeatDayOfWeek();

        List<LocalDate> dates = sessionDates(
                request.startDate(),
                request.endDate(),
                day
        );

        if (dates.isEmpty()) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "활동 기간 안에 반복 요일이 없습니다."
            );
        }

        Activity activity = activityRepository.save(
                new Activity(
                        member.getTeamId(),
                        member.getId(),
                        request.type(),
                        request.name(),
                        request.description(),
                        request.targetMajors(),
                        generationIds,
                        request.capacity(),
                        request.startDate(),
                        request.endDate(),
                        day,
                        request.repeatTime(),
                        request.recruitMode(),
                        request.topicMode(),
                        request.topicOpenDaysBefore() == null
                                ? DEFAULT_TOPIC_OPEN_DAYS
                                : request.topicOpenDaysBefore()
                )
        );

        activityMemberRepository.save(
                new ActivityMember(
                        activity.getId(),
                        member.getId(),
                        ActivityRole.LEADER
                )
        );

        List<ActivitySession> sessions = new ArrayList<>();

        for (int i = 0; i < dates.size(); i++) {
            sessions.add(
                    new ActivitySession(
                            activity.getId(),
                            i + 1,
                            dates.get(i),
                            request.repeatTime()
                    )
            );
        }

        sessionRepository.saveAll(sessions);

        return ActivitySummaryResponse.of(activity, 1);
    }

    public ActivityDetailResponse getActivity(
            Long loginMemberId,
            Long activityId
    ) {
        Member member = getMember(loginMemberId);
        Activity activity = getActivityOf(member, activityId);

        List<ActivityMember> members =
                activityMemberRepository.findAllByActivityIdOrderByJoinedAtAsc(
                        activityId
                );

        Map<Long, String> names = memberNames(
                members.stream()
                        .map(ActivityMember::getMemberId)
                        .collect(Collectors.toSet())
        );

        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        List<SessionResponse> upcoming =
                sessionRepository
                        .findTop3ByActivityIdAndStatusAndSessionDateGreaterThanEqualOrderBySessionDateAsc(
                                activityId,
                                SessionStatus.SCHEDULED,
                                today
                        )
                        .stream()
                        .map(s -> SessionResponse.of(
                                s,
                                isVotingOpen(activity, s, now)
                        ))
                        .toList();

        return ActivityDetailResponse.of(
                activity,
                ActivitySummaryResponse.of(activity, members.size()),
                members.stream()
                        .map(m -> ActivityMemberResponse.of(
                                m,
                                names.get(m.getMemberId())
                        ))
                        .toList(),
                upcoming
        );
    }

    @Transactional
    public void updateActivity(
            Long loginMemberId,
            Long activityId,
            ActivityUpdateRequest request
    ) {
        Member member = getMember(loginMemberId);
        Activity activity = getActivityForUpdate(member, activityId);

        requireManager(member, activityId);

        if (request.targetGenerationIds() != null) {
            validateGenerations(member, request.targetGenerationIds());
        }

        if (request.capacity() != null
                && request.capacity()
                < activityMemberRepository.countByActivityId(activityId)) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "현재 참여자 수보다 모집 인원을 줄일 수 없습니다."
            );
        }

        // 기간은 회차가 이미 생성돼 있어 수정하지 않는다.
        activity.update(
                request.name(),
                request.description(),
                request.targetMajors(),
                request.targetGenerationIds(),
                request.capacity(),
                null,
                null
        );

        if (request.status() != null) {
            activity.changeStatus(request.status());
        }
    }

    // 자식 데이터를 벌크 쿼리로 먼저 지운 뒤 활동 삭제
    @Transactional
    public void deleteActivity(
            Long loginMemberId,
            Long activityId
    ) {
        Member member = getMember(loginMemberId);
        Activity activity = getActivityForUpdate(member, activityId);

        requireManager(member, activityId);

        voteRepository.deleteAllByActivityId(activityId);
        candidateRepository.deleteAllByActivityId(activityId);
        sessionRepository.deleteAllByActivityId(activityId);
        applicationRepository.deleteAllByActivityId(activityId);
        activityMemberRepository.deleteAllByActivityId(activityId);

        activityRepository.delete(activity);
    }

    // ===== 참여 신청 =====

    // 활동 행을 잠가 정원 초과 및 중복 신청 방지
    @Transactional
    public ApplicationResponse apply(
            Long loginMemberId,
            Long activityId
    ) {
        Member member = getMember(loginMemberId);
        Activity activity = getActivityForUpdate(member, activityId);

        if (activity.getStatus() != ActivityStatus.RECRUITING) {
            throw error(
                    HttpStatus.CONFLICT,
                    "모집 중인 활동이 아닙니다."
            );
        }

        validateEligible(member, activity);

        if (activityMemberRepository.existsByActivityIdAndMemberId(
                activityId,
                member.getId()
        ) || applicationRepository.existsByActivityIdAndMemberId(
                activityId,
                member.getId()
        )) {
            throw error(
                    HttpStatus.CONFLICT,
                    "이미 신청했거나 참여 중인 활동입니다."
            );
        }

        ActivityApplication application;

        if (activity.getRecruitMode() == RecruitMode.FIRST_COME) {
            ensureCapacity(activity);

            application = applicationRepository.save(
                    new ActivityApplication(
                            activityId,
                            member.getId(),
                            ApplicationStatus.APPROVED
                    )
            );

            activityMemberRepository.save(
                    new ActivityMember(
                            activityId,
                            member.getId(),
                            ActivityRole.MEMBER
                    )
            );
        } else {
            application = applicationRepository.save(
                    new ActivityApplication(
                            activityId,
                            member.getId(),
                            ApplicationStatus.PENDING
                    )
            );
        }

        return ApplicationResponse.of(application, member.getName());
    }

    public List<ApplicationResponse> getApplications(
            Long loginMemberId,
            Long activityId
    ) {
        Member member = getMember(loginMemberId);

        getActivityOf(member, activityId);
        requireManager(member, activityId);

        List<ActivityApplication> applications =
                applicationRepository.findAllByActivityIdOrderByCreatedAtAsc(
                        activityId
                );

        Map<Long, String> names = memberNames(
                applications.stream()
                        .map(ActivityApplication::getMemberId)
                        .collect(Collectors.toSet())
        );

        return applications.stream()
                .map(a -> ApplicationResponse.of(
                        a,
                        names.get(a.getMemberId())
                ))
                .toList();
    }

    // 승인된 신청이면 참여자에서도 빠지고 내 투표도 함께 삭제
    // 스터디장은 역할 변경 후에만 취소 가능
    @Transactional
    public void cancelMyApplication(
            Long loginMemberId,
            Long activityId
    ) {
        Member member = getMember(loginMemberId);

        getActivityForUpdate(member, activityId);

        ActivityApplication application =
                applicationRepository
                        .findByActivityIdAndMemberId(
                                activityId,
                                member.getId()
                        )
                        .orElseThrow(() -> error(
                                HttpStatus.NOT_FOUND,
                                "존재하지 않는 참여 신청입니다."
                        ));

        activityMemberRepository
                .findByActivityIdAndMemberId(
                        activityId,
                        member.getId()
                )
                .ifPresent(am -> {
                    if (am.getRole() == ActivityRole.LEADER) {
                        throw error(
                                HttpStatus.CONFLICT,
                                "스터디장은 역할을 변경한 뒤 취소할 수 있습니다."
                        );
                    }

                    activityMemberRepository.delete(am);

                    voteRepository.deleteAllByActivityIdAndMemberId(
                            activityId,
                            member.getId()
                    );
                });

        applicationRepository.delete(application);
    }

    @Transactional
    public void approveApplication(
            Long loginMemberId,
            Long activityId,
            Long applicationId
    ) {
        Member member = getMember(loginMemberId);
        Activity activity = getActivityForUpdate(member, activityId);

        requireManager(member, activityId);

        ActivityApplication application =
                findApplication(applicationId, activityId);

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw error(
                    HttpStatus.CONFLICT,
                    "승인 대기 상태가 아닙니다."
            );
        }

        ensureCapacity(activity);

        application.approve();

        activityMemberRepository.save(
                new ActivityMember(
                        activityId,
                        application.getMemberId(),
                        ActivityRole.MEMBER
                )
        );
    }

    @Transactional
    public void rejectApplication(
            Long loginMemberId,
            Long activityId,
            Long applicationId
    ) {
        Member member = getMember(loginMemberId);

        getActivityForUpdate(member, activityId);
        requireManager(member, activityId);

        ActivityApplication application =
                findApplication(applicationId, activityId);

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw error(
                    HttpStatus.CONFLICT,
                    "승인 대기 상태가 아닙니다."
            );
        }

        application.reject();
    }

    // ===== 참여자 =====

    public List<ActivityMemberResponse> getMembers(
            Long loginMemberId,
            Long activityId
    ) {
        Member member = getMember(loginMemberId);

        getActivityOf(member, activityId);

        List<ActivityMember> members =
                activityMemberRepository.findAllByActivityIdOrderByJoinedAtAsc(
                        activityId
                );

        Map<Long, String> names = memberNames(
                members.stream()
                        .map(ActivityMember::getMemberId)
                        .collect(Collectors.toSet())
        );

        return members.stream()
                .map(m -> ActivityMemberResponse.of(
                        m,
                        names.get(m.getMemberId())
                ))
                .toList();
    }

    @Transactional
    public void changeMemberRole(
            Long loginMemberId,
            Long activityId,
            Long targetMemberId,
            MemberRoleUpdateRequest request
    ) {
        Member member = getMember(loginMemberId);

        getActivityForUpdate(member, activityId);
        requireManager(member, activityId);

        ActivityMember target =
                activityMemberRepository
                        .findByActivityIdAndMemberId(
                                activityId,
                                targetMemberId
                        )
                        .orElseThrow(() -> error(
                                HttpStatus.NOT_FOUND,
                                "활동 참여자가 아닙니다."
                        ));

        target.changeRole(request.role());
    }

    // ===== 회차 =====

    public List<SessionResponse> getSessions(
            Long loginMemberId,
            Long activityId
    ) {
        Member member = getMember(loginMemberId);
        Activity activity = getActivityOf(member, activityId);

        LocalDateTime now = LocalDateTime.now(clock);

        return sessionRepository
                .findAllByActivityIdOrderBySessionNoAsc(activityId)
                .stream()
                .map(s -> SessionResponse.of(
                        s,
                        isVotingOpen(activity, s, now)
                ))
                .toList();
    }

    public SessionResponse getSession(
            Long loginMemberId,
            Long activityId,
            Long sessionId
    ) {
        Member member = getMember(loginMemberId);
        Activity activity = getActivityOf(member, activityId);

        ActivitySession session = findSession(
                activityId,
                sessionId
        );

        return SessionResponse.of(
                session,
                isVotingOpen(
                        activity,
                        session,
                        LocalDateTime.now()
                )
        );
    }

    // 회차 행을 잠그고 수정한다.
    // 지정형의 기존 주제 입력 및 회차 수정 기능을 유지한다.
    @Transactional
    public void updateSession(
            Long loginMemberId,
            Long activityId,
            Long sessionId,
            SessionUpdateRequest request
    ) {
        Member member = getMember(loginMemberId);
        Activity activity = getActivityOf(member, activityId);

        requireManager(member, activityId);

        ActivitySession session =
                getSessionForUpdate(activityId, sessionId);

        LocalDate date = request.sessionDate();

        if (date != null
                && (date.isBefore(activity.getStartDate())
                || date.isAfter(activity.getEndDate()))) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "회차 날짜는 활동 기간 안이어야 합니다."
            );
        }

        session.update(
                request.topic(),
                date,
                request.sessionTime(),
                request.status()
        );
    }

    // ===== 주제 후보 / 투표 =====

    public List<TopicCandidateResponse> getCandidates(
            Long loginMemberId,
            Long activityId,
            Long sessionId
    ) {
        Member member = getMember(loginMemberId);

        getActivityOf(member, activityId);
        findSession(activityId, sessionId);

        List<TopicCandidate> candidates =
                candidateRepository.findAllBySessionIdOrderByIdAsc(
                        sessionId
                );

        Map<Long, Long> counts = new HashMap<>();

        voteRepository.countBySessionIds(List.of(sessionId))
                .forEach(c -> counts.put(
                        c.candidateId(),
                        c.count()
                ));

        Long myCandidateId =
                voteRepository
                        .findBySessionIdAndMemberId(
                                sessionId,
                                member.getId()
                        )
                        .map(TopicVote::getCandidateId)
                        .orElse(null);

        Map<Long, String> names = memberNames(
                candidates.stream()
                        .map(TopicCandidate::getProposerId)
                        .collect(Collectors.toSet())
        );

        return candidates.stream()
                .map(c -> new TopicCandidateResponse(
                        c.getId(),
                        c.getTitle(),
                        c.getProposerId(),
                        names.get(c.getProposerId()),
                        counts.getOrDefault(c.getId(), 0L),
                        c.getId().equals(myCandidateId)
                ))
                .sorted(
                        Comparator.comparingLong(
                                        TopicCandidateResponse::voteCount
                                )
                                .reversed()
                                .thenComparing(
                                        TopicCandidateResponse::id
                                )
                )
                .toList();
    }

    @Transactional
    public TopicCandidateResponse proposeCandidate(
            Long loginMemberId,
            Long activityId,
            Long sessionId,
            TopicCandidateCreateRequest request
    ) {
        Member member = getMember(loginMemberId);
        Activity activity = getActivityOf(member, activityId);

        requireParticipant(member, activityId);

        ActivitySession session =
                getSessionForUpdate(activityId, sessionId);

        requireVotingOpen(activity, session);

        String title = request.title().trim();

        if (candidateRepository.existsBySessionIdAndTitle(
                sessionId,
                title
        )) {
            throw error(
                    HttpStatus.CONFLICT,
                    "이미 제안된 주제입니다."
            );
        }

        TopicCandidate candidate = candidateRepository.save(
                new TopicCandidate(
                        sessionId,
                        member.getId(),
                        title
                )
        );

        return new TopicCandidateResponse(
                candidate.getId(),
                candidate.getTitle(),
                member.getId(),
                member.getName(),
                0,
                false
        );
    }

    // 회차당 1표
    // 회차 행 락과 (session_id, member_id) 유니크 제약으로 중복 투표 방지
    @Transactional
    public void vote(
            Long loginMemberId,
            Long activityId,
            Long sessionId,
            Long candidateId
    ) {
        Member member = getMember(loginMemberId);
        Activity activity = getActivityOf(member, activityId);

        requireParticipant(member, activityId);

        ActivitySession session =
                getSessionForUpdate(activityId, sessionId);

        requireVotingOpen(activity, session);

        candidateRepository
                .findByIdAndSessionId(candidateId, sessionId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 주제 후보입니다."
                ));

        if (voteRepository.findBySessionIdAndMemberId(
                sessionId,
                member.getId()
        ).isPresent()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "이미 투표했습니다. 취소 후 다시 투표해 주세요."
            );
        }

        voteRepository.save(
                new TopicVote(
                        sessionId,
                        candidateId,
                        member.getId()
                )
        );
    }

    @Transactional
    public void cancelVote(
            Long loginMemberId,
            Long activityId,
            Long sessionId,
            Long candidateId
    ) {
        Member member = getMember(loginMemberId);
        Activity activity = getActivityOf(member, activityId);

        requireParticipant(member, activityId);

        ActivitySession session =
                getSessionForUpdate(activityId, sessionId);

        requireVotingOpen(activity, session);

        TopicVote vote =
                voteRepository
                        .findBySessionIdAndMemberIdAndCandidateId(
                                sessionId,
                                member.getId(),
                                candidateId
                        )
                        .orElseThrow(() -> error(
                                HttpStatus.NOT_FOUND,
                                "존재하지 않는 투표입니다."
                        ));

        voteRepository.delete(vote);
    }

    // 스케줄러용:
    // 투표형 회차의 마감 시각이 지나면 최다 득표 주제를 확정한다.
    // 동률이면 먼저 제안된 후보를 선택한다.
    @Transactional
    public int confirmClosedTopics() {
        LocalDateTime now = LocalDateTime.now(clock);

        // 오늘 날짜까지의 미확정 투표형 회차를 조회한다.
        List<ActivitySession> sessions =
                sessionRepository.findClosedVoteSessions(
                        now.toLocalDate(),
                        SessionStatus.SCHEDULED,
                        TopicMode.VOTE
                );

        // 회차 당일 오후 3시가 지난 회차만 처리한다.
        sessions = sessions.stream()
                .filter(session -> !now.isBefore(
                        session.getSessionDate().atTime(
                                TOPIC_VOTE_DEADLINE_HOUR,
                                0
                        )
                ))
                .toList();

        if (sessions.isEmpty()) {
            return 0;
        }

        Set<Long> sessionIds = sessions.stream()
                .map(ActivitySession::getId)
                .collect(Collectors.toSet());

        Map<Long, List<VoteCount>> votesBySession =
                voteRepository.countBySessionIds(sessionIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                VoteCount::sessionId
                        ));

        Map<Long, TopicCandidate> candidates = new HashMap<>();

        candidateRepository.findAllBySessionIdIn(sessionIds)
                .forEach(candidate ->
                        candidates.put(
                                candidate.getId(),
                                candidate
                        )
                );

        int confirmed = 0;

        for (ActivitySession session : sessions) {
            List<VoteCount> votes =
                    votesBySession.get(session.getId());

            // 득표가 없는 회차는 자동 확정하지 않는다.
            if (votes == null || votes.isEmpty()) {
                continue;
            }

            // 득표수가 가장 많은 후보를 선택한다.
            // 동점이면 후보 ID가 더 작은 후보를 선택한다.
            VoteCount best = votes.stream()
                    .max(
                            Comparator.comparingLong(
                                            VoteCount::count
                                    )
                                    .thenComparing(
                                            VoteCount::candidateId,
                                            Comparator.reverseOrder()
                                    )
                    )
                    .orElse(null);

            TopicCandidate winner =
                    best == null
                            ? null
                            : candidates.get(best.candidateId());

            if (winner == null) {
                continue;
            }

            session.confirmTopic(winner.getTitle());
            confirmed++;
        }

        return confirmed;
    }

    // ===== 공통 =====

    // 가입 완료(JOINED)된 팀원만 사용 가능
    private Member getMember(Long loginMemberId) {
        Member member = memberRepository.findById(loginMemberId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 멤버입니다."
                ));

        if (member.getStatus() != MemberStatus.JOINED) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "가입이 완료된 팀원만 사용할 수 있습니다."
            );
        }

        return member;
    }

    // 다른 팀의 활동은 존재하지 않는 것처럼 404
    private Activity getActivityOf(
            Member member,
            Long activityId
    ) {
        return activityRepository.findById(activityId)
                .filter(a -> a.getTeamId().equals(member.getTeamId()))
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 활동입니다."
                ));
    }

    // 변경 작업용: 활동 행을 잠그고 조회
    private Activity getActivityForUpdate(
            Member member,
            Long activityId
    ) {
        return activityRepository.findWithLockById(activityId)
                .filter(a -> a.getTeamId().equals(member.getTeamId()))
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 활동입니다."
                ));
    }

    private ActivitySession findSession(
            Long activityId,
            Long sessionId
    ) {
        return sessionRepository
                .findByIdAndActivityId(sessionId, activityId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 회차입니다."
                ));
    }

    private ActivitySession getSessionForUpdate(
            Long activityId,
            Long sessionId
    ) {
        return sessionRepository.findWithLockById(sessionId)
                .filter(s -> s.getActivityId().equals(activityId))
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 회차입니다."
                ));
    }

    private ActivityApplication findApplication(
            Long applicationId,
            Long activityId
    ) {
        return applicationRepository
                .findByIdAndActivityId(applicationId, activityId)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 참여 신청입니다."
                ));
    }

    // 부장은 모든 활동의 스터디장 권한
    private void requireManager(
            Member member,
            Long activityId
    ) {
        if (member.isLeader()) {
            return;
        }

        boolean activityLeader =
                activityMemberRepository
                        .findByActivityIdAndMemberId(
                                activityId,
                                member.getId()
                        )
                        .map(m -> m.getRole() == ActivityRole.LEADER)
                        .orElse(false);

        if (!activityLeader) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "스터디장 또는 부장만 사용할 수 있는 기능입니다."
            );
        }
    }

    private void requireParticipant(
            Member member,
            Long activityId
    ) {
        if (!activityMemberRepository.existsByActivityIdAndMemberId(
                activityId,
                member.getId()
        )) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "활동 참여자만 사용할 수 있는 기능입니다."
            );
        }
    }

    // 투표형 후보 제안 및 투표 가능 기간 확인
    // 회차 날짜 기준 N일 전부터 회차 당일 15:00 전까지 허용한다.
    private boolean isVotingOpen(
            Activity activity,
            ActivitySession session,
            LocalDateTime now
    ) {
        if (activity.getTopicMode() != TopicMode.VOTE
                || session.getStatus() != SessionStatus.SCHEDULED
                || session.getTopic() != null) {
            return false;
        }

        // 투표 시작일: 회차 날짜에서 설정된 일수만큼 이전
        LocalDateTime openAt = session.getSessionDate()
                .minusDays(activity.getTopicOpenDaysBefore())
                .atStartOfDay();

        // 투표 마감: 회차 당일 오후 3시
        LocalDateTime deadline = session.getSessionDate()
                .atTime(TOPIC_VOTE_DEADLINE_HOUR, 0);

        return !now.isBefore(openAt)
                && now.isBefore(deadline);
    }

    private void requireVotingOpen(
            Activity activity,
            ActivitySession session
    ) {
        if (!isVotingOpen(
                activity,
                session,
                LocalDateTime.now()
        )) {
            throw error(
                    HttpStatus.CONFLICT,
                    "투표가 열려 있는 회차가 아닙니다."
            );
        }
    }

    private void ensureCapacity(Activity activity) {
        if (activityMemberRepository.countByActivityId(
                activity.getId()
        ) >= activity.getCapacity()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "모집 인원이 가득 찼습니다."
            );
        }
    }

    // 대상 전공/기수가 지정된 활동이면 해당하는 멤버만 신청 가능
    private void validateEligible(
            Member member,
            Activity activity
    ) {
        Set<Major> majors = activity.getTargetMajors();

        if (!majors.isEmpty()
                && Collections.disjoint(majors, member.getMajors())) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "대상 전공이 아닙니다."
            );
        }

        Set<Long> generationIds =
                activity.getTargetGenerationIds();

        if (!generationIds.isEmpty()
                && (member.getGeneration() == null
                || !generationIds.contains(
                member.getGeneration().getId()
        ))) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "대상 기수가 아닙니다."
            );
        }
    }

    // 기수는 같은 팀의 기수만 지정 가능
    // 한 번의 쿼리로 검증
    private void validateGenerations(
            Member member,
            Set<Long> generationIds
    ) {
        if (generationIds.isEmpty()) {
            return;
        }

        List<Generation> found =
                generationRepository.findAllById(generationIds);

        boolean valid =
                found.size() == generationIds.size()
                        && found.stream().allMatch(
                        g -> g.getTeamId().equals(
                                member.getTeamId()
                        )
                );

        if (!valid) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "존재하지 않는 기수입니다."
            );
        }
    }

    private void validatePeriod(
            LocalDate start,
            LocalDate end
    ) {
        if (start.isAfter(end)) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "시작일은 종료일보다 늦을 수 없습니다."
            );
        }

        if (ChronoUnit.DAYS.between(start, end)
                > MAX_PERIOD_DAYS) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "활동 기간은 최대 1년까지 설정할 수 있습니다."
            );
        }
    }

    private List<LocalDate> sessionDates(
            LocalDate start,
            LocalDate end,
            DayOfWeek day
    ) {
        List<LocalDate> dates = new ArrayList<>();

        for (
                LocalDate d = start.with(
                        TemporalAdjusters.nextOrSame(day)
                );
                !d.isAfter(end);
                d = d.plusWeeks(1)
        ) {
            dates.add(d);
        }

        return dates;
    }

    // 인원수는 group by 한 번으로 조회
    // 활동마다 count 쿼리가 나가지 않게 한다.
    private List<ActivitySummaryResponse> toSummaries(
            List<Activity> activities
    ) {
        if (activities.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> counts = new HashMap<>();

        activityMemberRepository.countByActivityIds(
                        activities.stream()
                                .map(Activity::getId)
                                .toList()
                )
                .forEach(c -> counts.put(
                        c.activityId(),
                        c.count()
                ));

        return activities.stream()
                .map(a -> ActivitySummaryResponse.of(
                        a,
                        counts.getOrDefault(a.getId(), 0L)
                ))
                .toList();
    }

    // Collectors.toMap은 value가 null이면 NPE가 나므로 직접 담는다.
    // 이름이 null인 멤버에 대비한다.
    private Map<Long, String> memberNames(
            Set<Long> memberIds
    ) {
        Map<Long, String> names = new HashMap<>();

        if (memberIds.isEmpty()) {
            return names;
        }

        memberRepository.findAllById(memberIds)
                .forEach(m -> names.put(
                        m.getId(),
                        m.getName()
                ));

        return names;
    }

    private CustomException error(
            HttpStatus status,
            String message
    ) {
        return new CustomException(status, message);
    }
}
package pv.domain.imports.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.activity.repository.ActivityRepository;
import pv.domain.imports.dto.request.ImportConfirmRequest;
import pv.domain.imports.dto.request.ImportDocumentType;
import pv.domain.imports.dto.response.ImportConfirmResponse;
import pv.domain.imports.entity.ImportDocument;
import pv.domain.imports.entity.ImportJob;
import pv.domain.imports.entity.ImportStatus;
import pv.domain.imports.repository.ImportDocumentRepository;
import pv.domain.imports.repository.ImportJobRepository;
import pv.domain.interviews.entity.InterviewAnswer;
import pv.domain.interviews.entity.InterviewQuestion;
import pv.domain.interviews.repository.InterviewAnswerRepository;
import pv.domain.interviews.repository.InterviewQuestionRepository;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.MemberStatus;
import pv.domain.member.repository.MemberRepository;
import pv.domain.posts.entity.Post;
import pv.domain.posts.repository.PostRepository;
import pv.domain.worklogs.entity.Worklog;
import pv.domain.worklogs.repository.WorklogRepository;
import pv.global.entity.ContentSource;
import pv.global.exception.CustomException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImportConfirmService {

    private static final int WORKLOG_TITLE_MAX = 100;

    private final ImportJobRepository jobRepository;
    private final ImportDocumentRepository documentRepository;
    private final MemberRepository memberRepository;
    private final ActivityRepository activityRepository;
    private final PostRepository postRepository;
    private final WorklogRepository worklogRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final Clock clock;

    @Transactional
    public ImportConfirmResponse confirm(Long memberId, Long importId, ImportConfirmRequest request) {
        Member me = findJoinedMember(memberId);

        // 행 잠금: 같은 가져오기의 동시 확정은 순서대로 처리됨
        ImportJob job = jobRepository.findByIdAndMemberIdForUpdate(importId, memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "가져오기 기록을 찾을 수 없습니다."));
        checkStatus(job);

        Map<Long, ImportDocument> docs = documentRepository.findAllByImportIdOrderByIdAsc(importId).stream()
                .collect(Collectors.toMap(ImportDocument::getId, Function.identity()));

        List<ImportConfirmRequest.Item> items = request.documents();
        validateItems(items, docs);
        validateAuthors(me, items);
        validateActivities(me, items);

        LocalDateTime now = LocalDateTime.now(clock);
        List<Post> posts = new ArrayList<>();
        List<Worklog> worklogs = new ArrayList<>();
        List<ImportConfirmRequest.Item> interviews = new ArrayList<>();

        for (ImportConfirmRequest.Item item : items) {
            ImportDocument doc = docs.get(item.documentId());
            Long authorId = item.authorId() != null ? item.authorId() : memberId;
            LocalDateTime createdAt = item.createdAt() != null ? item.createdAt() : now;
            if (createdAt.isAfter(now)) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "작성일은 미래로 지정할 수 없습니다.");
            }

            switch (item.type()) {
                case POST -> posts.add(Post.createImported(
                        authorId, item.activityId(), doc.getTitle(), doc.getContent(), createdAt));
                case WORKLOG -> worklogs.add(Worklog.builder()
                        .author(memberRepository.getReferenceById(authorId)) // 쿼리 없이 연결
                        .title(cut(doc.getTitle(), WORKLOG_TITLE_MAX))
                        .content(doc.getContent())
                        .visibility(item.visibility())
                        .createdAt(createdAt)
                        .source(ContentSource.NOTION)
                        .build());
                case INTERVIEW -> interviews.add(item);
            }
        }

        try {
            postRepository.saveAll(posts);
            worklogRepository.saveAll(worklogs);
            int[] interviewResult = saveInterviews(interviews, docs, memberId, now);

            documentRepository.deleteAllByImportId(importId); // 임시 문서 일괄 삭제
            job.confirm();

            return new ImportConfirmResponse(importId, posts.size(), worklogs.size(),
                    interviewResult[0], interviewResult[1], interviewResult[2]);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "다른 요청과 충돌했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    // 면접 질문: 기존 질문과 내용이 일치할 때만 답변을 연결 (공용 질문 풀이라 새 질문은 만들지 않음)
    // 반환값: [저장된 개수, 이미 답변이 있어 건너뛴 개수, 일치하는 질문이 없어 건너뛴 개수]
    private int[] saveInterviews(List<ImportConfirmRequest.Item> interviews,
                                 Map<Long, ImportDocument> docs, Long memberId, LocalDateTime now) {
        if (interviews.isEmpty()) {
            return new int[]{0, 0, 0};
        }

        Set<String> contents = interviews.stream()
                .map(i -> docs.get(i.documentId()).getTitle().strip())
                .collect(Collectors.toSet());

        Map<String, InterviewQuestion> questions = questionRepository.findAllByContentIn(contents).stream()
                .collect(Collectors.toMap(InterviewQuestion::getContent, Function.identity()));
        if (questions.isEmpty()) {
            return new int[]{0, 0, interviews.size()};
        }

        // 이미 답변이 있는 (질문, 회원) 쌍을 한 번에 조회
        Set<Long> questionIds = questions.values().stream()
                .map(InterviewQuestion::getId)
                .collect(Collectors.toSet());
        Set<String> answered = new HashSet<>();
        for (Object[] row : answerRepository.findAnsweredPairs(questionIds)) {
            answered.add(row[0] + ":" + row[1]);
        }

        List<InterviewAnswer> answers = new ArrayList<>();
        int skipped = 0;
        int unmatched = 0;
        for (ImportConfirmRequest.Item item : interviews) {
            ImportDocument doc = docs.get(item.documentId());
            InterviewQuestion question = questions.get(doc.getTitle().strip());
            if (question == null) {
                unmatched++;
                continue;
            }
            Long authorId = item.authorId() != null ? item.authorId() : memberId;
            if (!answered.add(question.getId() + ":" + authorId)) {
                skipped++;
                continue;
            }
            LocalDateTime createdAt = item.createdAt() != null ? item.createdAt() : now;
            answers.add(new InterviewAnswer(question, memberRepository.getReferenceById(authorId),
                    doc.getContent(), createdAt, ContentSource.NOTION));
        }
        answerRepository.saveAll(answers);
        return new int[]{answers.size(), skipped, unmatched};
    }

    private void checkStatus(ImportJob job) {
        if (job.getStatus() == ImportStatus.CONFIRMED) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 확정된 가져오기입니다.");
        }
        if (job.getStatus() != ImportStatus.READY) {
            throw new CustomException(HttpStatus.CONFLICT, "처리가 끝난 가져오기만 확정할 수 있습니다.");
        }
    }

    // 문서 ID 중복·존재 여부, 면접 질문의 카테고리 필수 검증
    private void validateItems(List<ImportConfirmRequest.Item> items, Map<Long, ImportDocument> docs) {
        Set<Long> seen = new HashSet<>();
        for (ImportConfirmRequest.Item item : items) {
            if (!docs.containsKey(item.documentId())) {
                throw new CustomException(HttpStatus.BAD_REQUEST,
                        "이 가져오기에 없는 문서입니다. (documentId=" + item.documentId() + ")");
            }
            if (!seen.add(item.documentId())) {
                throw new CustomException(HttpStatus.BAD_REQUEST,
                        "같은 문서를 중복 지정할 수 없습니다. (documentId=" + item.documentId() + ")");
            }
        }
    }

    // 일반 부원은 본인만, 부장은 같은 팀 가입 회원 누구든 작성자로 지정 가능
    private void validateAuthors(Member me, List<ImportConfirmRequest.Item> items) {
        Set<Long> others = items.stream()
                .map(ImportConfirmRequest.Item::authorId)
                .filter(id -> id != null && !id.equals(me.getId()))
                .collect(Collectors.toSet());
        if (others.isEmpty()) {
            return;
        }
        if (!me.isLeader()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "다른 사람을 작성자로 지정할 수 있는 건 부장뿐입니다.");
        }
        Set<Long> valid = new HashSet<>(memberRepository.findJoinedIds(me.getTeamId(), others));
        if (!valid.containsAll(others)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "작성자로 지정할 수 없는 회원이 포함되어 있습니다.");
        }
    }

    // 정리글에 지정한 활동이 우리 팀 활동인지 한 번에 확인 (업무일지·면접 질문은 활동 무시)
    private void validateActivities(Member me, List<ImportConfirmRequest.Item> items) {
        Set<Long> activityIds = items.stream()
                .filter(i -> i.type() == ImportDocumentType.POST && i.activityId() != null)
                .map(ImportConfirmRequest.Item::activityId)
                .collect(Collectors.toSet());
        if (activityIds.isEmpty()) {
            return;
        }
        Set<Long> teamActivities = new HashSet<>(activityRepository.findIdsByTeamId(me.getTeamId()));
        if (!teamActivities.containsAll(activityIds)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "존재하지 않거나 접근할 수 없는 활동이 포함되어 있습니다.");
        }
    }

    private Member findJoinedMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회원이 없습니다."));
        if (member.getStatus() != MemberStatus.JOINED) {
            throw new CustomException(HttpStatus.FORBIDDEN, "가입 승인 후 이용할 수 있습니다.");
        }
        return member;
    }

    private String cut(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
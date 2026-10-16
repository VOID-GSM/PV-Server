package pv.domain.interviews.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pv.domain.interviews.dto.AnswerResponse;
import pv.domain.interviews.dto.QuestionCreateRequest;
import pv.domain.interviews.dto.QuestionDetailResponse;
import pv.domain.interviews.dto.QuestionResponse;
import pv.domain.interviews.entity.InterviewAnswer;
import pv.domain.interviews.entity.InterviewCategory;
import pv.domain.interviews.entity.InterviewQuestion;
import pv.domain.interviews.repository.InterviewAnswerRepository;
import pv.domain.interviews.repository.InterviewQuestionRepository;
import pv.domain.member.entity.Member;
import pv.global.exception.CustomException;
import pv.domain.interviews.dto.AnswerRequest;
import pv.domain.member.repository.MemberRepository;
import pv.domain.member.entity.Major;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {
    private static final int MAX_RANDOM_COUNT = 20;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final MemberRepository memberRepository;

    public List<QuestionResponse> getQuestions(Major major, InterviewCategory category) {
        return questionRepository.search(major, category).stream().map(QuestionResponse::from).toList();
    }

    @Transactional
    public Long create(Long memberId, QuestionCreateRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        if (!member.isJoined()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "승인된 팀원만 질문을 등록할 수 있습니다.");
        }

        String content = req.content().strip();
        if (questionRepository.existsByContent(content)) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 등록된 질문입니다.");
        }
        try {
            return questionRepository.saveAndFlush(
                    new InterviewQuestion(content, req.major(), req.category())).getId();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 등록된 질문입니다.");
        }
    }

    @Transactional
    public InterviewQuestion registerFromActivity(String content, Major major, InterviewCategory category) {
        String normalized = content.strip();
        return questionRepository.findByContent(normalized)
                .orElseGet(() -> questionRepository.save(new InterviewQuestion(normalized, major, category)));
    }

    public QuestionDetailResponse getQuestion(Long id) {
        return new QuestionDetailResponse(QuestionResponse.from(find(id)), answerRepository.countByQuestionId(id));
    }

    public List<AnswerResponse> getAnswers(Long id) {
        find(id);
        return answerRepository.findAllResponses(id);
    }

    @Transactional
    public AnswerResponse createAnswer(Long memberId, Long questionId, AnswerRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
        if (!member.isJoined()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "승인된 팀원만 답변할 수 있습니다.");
        }

        InterviewQuestion question = find(questionId);
        if (answerRepository.existsByQuestionIdAndMemberId(questionId, memberId)) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 답변한 질문입니다.");
        }

        try {
            InterviewAnswer saved = answerRepository.saveAndFlush(
                    new InterviewAnswer(question, member, req.content()));
            return AnswerResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 답변한 질문입니다.");
        }
    }

    @Transactional
    public void updateAnswer(Long memberId, Long questionId, Long answerId, AnswerRequest req) {
        InterviewAnswer answer = answerRepository.findById(answerId)
                .filter(a -> a.belongsTo(questionId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "답변을 찾을 수 없습니다."));
        if (!answer.isWrittenBy(memberId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "본인 답변만 수정할 수 있습니다.");
        }
        answer.updateContent(req.content());
    }

    public List<QuestionResponse> getRandom(Major major, InterviewCategory category, int count) {
        if (count < 1 || count > MAX_RANDOM_COUNT) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "질문 개수는 1~" + MAX_RANDOM_COUNT + "개 사이여야 합니다.");
        }
        return questionRepository.findRandom(major, category, PageRequest.of(0, count)).stream()
                .map(QuestionResponse::from)
                .toList();
    }

    private InterviewQuestion find(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "질문이 없습니다."));
    }
}

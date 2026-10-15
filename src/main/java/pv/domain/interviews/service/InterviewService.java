package pv.domain.interviews.service;

import lombok.RequiredArgsConstructor;
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
import pv.global.exception.CustomException;
import pv.domain.interviews.dto.AnswerRequest;
import pv.domain.member.repository.MemberRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {
    private static final int MAX_RANDOM_COUNT = 20;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final MemberRepository memberRepository;

    public List<QuestionResponse> getQuestions(InterviewQuestion.Major major, InterviewCategory category) {
        return questionRepository.search(major, category).stream().map(QuestionResponse::from).toList();
    }

    @Transactional
    public Long create(QuestionCreateRequest req) {
        if (questionRepository.findByContent(req.content()).isPresent())
            throw new CustomException(HttpStatus.CONFLICT, "이미 등록된 질문입니다.");
        return questionRepository.save(new InterviewQuestion(req.content(), req.major(), req.category())).getId();
    }

    @Transactional
    public InterviewQuestion registerFromActivity(String content, InterviewQuestion.Major major, InterviewCategory category) {
        return questionRepository.findByContent(content)
                .orElseGet(() -> questionRepository.save(new InterviewQuestion(content, major, category)));
    }

    public QuestionDetailResponse getQuestion(Long id) {
        return new QuestionDetailResponse(QuestionResponse.from(find(id)), answerRepository.countByQuestionId(id));
    }

    public List<AnswerResponse> getAnswers(Long id) {
        find(id);
        return answerRepository.findAllWithMember(id).stream().map(AnswerResponse::from).toList();
    }

    @Transactional
    public AnswerResponse createAnswer(Long memberId, Long questionId, AnswerRequest req) {
        InterviewQuestion question = find(questionId);
        if (answerRepository.existsByQuestionIdAndMemberId(questionId, memberId)) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 답변한 질문입니다.");
        }
        InterviewAnswer answer = new InterviewAnswer(
                question,
                memberRepository.getReferenceById(memberId),
                req.content());
        return AnswerResponse.from(answerRepository.save(answer));
    }

    public List<QuestionResponse> getRandom(InterviewQuestion.Major major, InterviewCategory category, int count) {
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

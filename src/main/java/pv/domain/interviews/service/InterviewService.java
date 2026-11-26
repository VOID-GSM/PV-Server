package pv.domain.interviews.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pv.domain.interviews.dto.*;
import pv.domain.interviews.entity.InterviewCategory;
import pv.domain.interviews.entity.InterviewQuestion;
import pv.domain.interviews.repository.InterviewAnswerRepository;
import pv.domain.interviews.repository.InterviewQuestionRepository;
import pv.domain.member.entity.Member;
import pv.domain.member.entity.Major;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static pv.domain.interviews.service.InterviewAuthorizer.checkOwnerOrAdmin;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;

    public List<QuestionResponse> getQuestions(Major major, InterviewCategory category) {
        return questionRepository.search(major, category).stream().map(QuestionResponse::from).toList();
    }

    public QuestionDetailResponse getQuestion(Long id) {
        return new QuestionDetailResponse(QuestionResponse.from(find(id)), answerRepository.countByQuestionId(id));
    }

    public List<AnswerResponse> getAnswers(Long id) {
        find(id);
        return answerRepository.findAllWithMember(id).stream().map(AnswerResponse::from).toList();
    }

    public List<QuestionResponse> getRandom(Long groupId, Major major, InterviewCategory category, int count) {
        return questionRepository.findRandom(groupId, major, category, PageRequest.of(0, count))
                .stream().map(QuestionResponse::from).toList();
    }

    @Transactional
    public void delete(Long id, Member me) {
        InterviewQuestion question = find(id);
        checkOwnerOrAdmin(question.getCreatedBy(), me);
        if (answerRepository.existsByQuestionId(id))
            throw new ResponseStatusException(CONFLICT, "답변이 있는 질문은 삭제할 수 없습니다.");
        questionRepository.delete(question);
    }

    private InterviewQuestion find(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "질문이 없습니다."));
    }
}

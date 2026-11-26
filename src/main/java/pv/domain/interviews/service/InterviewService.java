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
import pv.domain.member.repository.MemberRepository;

import java.util.List;

import static org.springframework.http.HttpStatus.*;
import static pv.domain.interviews.service.InterviewAuthorizer.checkOwnerOrLeader;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final MemberRepository memberRepository;

    public List<QuestionResponse> getQuestions(Major major, InterviewCategory category, Long memberId) {
        return questionRepository.search(member(memberId).getTeamId(), major, category).stream()
                .map(QuestionResponse::from).toList();
    }

    public QuestionDetailResponse getQuestion(Long id, Long memberId) {
        InterviewQuestion question = find(id, member(memberId));
        return new QuestionDetailResponse(QuestionResponse.from(question), answerRepository.countByQuestionId(id));
    }

    public List<AnswerResponse> getAnswers(Long id, Long memberId) {
        find(id, member(memberId));
        return answerRepository.findAllWithMember(id).stream().map(AnswerResponse::from).toList();
    }

    public List<QuestionResponse> getRandom(Long groupId, Major major, InterviewCategory category,
                                            int count, Long memberId) {
        return questionRepository.findRandom(member(memberId).getTeamId(), groupId, major, category,
                        PageRequest.of(0, count))
                .stream().map(QuestionResponse::from).toList();
    }

    @Transactional
    public void delete(Long id, Long memberId) {
        Member me = member(memberId);
        InterviewQuestion question = find(id, me);
        checkOwnerOrLeader(question.getCreatedBy(), me);
        if (answerRepository.existsByQuestionId(id))
            throw new ResponseStatusException(CONFLICT, "답변이 있는 질문은 삭제할 수 없습니다.");
        questionRepository.delete(question);
    }

    private Member member(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "회원 정보가 없습니다."));
    }

    private InterviewQuestion find(Long id, Member me) {
        return questionRepository.findById(id)
                .filter(q -> q.getGroup().getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "질문이 없습니다."));
    }
}

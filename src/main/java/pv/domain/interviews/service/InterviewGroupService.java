package pv.domain.interviews.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pv.domain.interviews.dto.GroupRequest;
import pv.domain.interviews.dto.GroupResponse;
import pv.domain.interviews.dto.QuestionCreateRequest;
import pv.domain.interviews.dto.QuestionResponse;
import pv.domain.interviews.entity.InterviewGroup;
import pv.domain.interviews.entity.InterviewQuestion;
import pv.domain.interviews.repository.InterviewAnswerRepository;
import pv.domain.interviews.repository.InterviewGroupRepository;
import pv.domain.interviews.repository.InterviewQuestionRepository;
import pv.domain.member.entity.Member;
import pv.domain.member.repository.MemberRepository;

import java.util.List;

import static org.springframework.http.HttpStatus.*;
import static pv.domain.interviews.service.InterviewAuthorizer.checkOwnerOrLeader;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewGroupService {
    private final InterviewGroupRepository groupRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long create(GroupRequest req, Long memberId) {
        return groupRepository.save(new InterviewGroup(req.name(), member(memberId))).getId();
    }

    public List<GroupResponse> getGroups(Long memberId) {
        return groupRepository.findAllWithQuestionCount(member(memberId).getTeamId());
    }

    @Transactional
    public void rename(Long groupId, GroupRequest req, Long memberId) {
        Member me = member(memberId);
        InterviewGroup group = find(groupId, me);
        checkOwnerOrLeader(group.getCreatedBy(), me);
        group.rename(req.name());
    }

    @Transactional
    public void delete(Long groupId, Long memberId) {
        Member me = member(memberId);
        InterviewGroup group = find(groupId, me);
        checkOwnerOrLeader(group.getCreatedBy(), me);
        if (answerRepository.existsByQuestionGroupId(groupId))
            throw new ResponseStatusException(CONFLICT, "답변이 달린 질문이 있는 그룹은 삭제할 수 없습니다.");
        questionRepository.deleteAllByGroupId(groupId);
        groupRepository.delete(group);
    }

    @Transactional
    public Long createQuestion(Long groupId, QuestionCreateRequest req, Long memberId) {
        Member me = member(memberId);
        InterviewGroup group = find(groupId, me);
        if (questionRepository.existsByGroupIdAndContent(groupId, req.content()))
            throw new ResponseStatusException(CONFLICT, "그룹에 이미 있는 질문입니다.");
        return questionRepository.save(
                new InterviewQuestion(group, me, req.content(), req.major(), req.category())).getId();
    }

    public List<QuestionResponse> getQuestions(Long groupId, Long memberId) {
        find(groupId, member(memberId));
        return questionRepository.findAllByGroupIdOrderByIdDesc(groupId).stream()
                .map(QuestionResponse::from).toList();
    }

    private Member member(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "회원 정보가 없습니다."));
    }

    private InterviewGroup find(Long groupId, Member me) {
        return groupRepository.findById(groupId)
                .filter(g -> g.getTeamId().equals(me.getTeamId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "그룹이 없습니다."));
    }
}

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

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static pv.domain.interviews.service.InterviewAuthorizer.checkOwnerOrAdmin;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewGroupService {
    private final InterviewGroupRepository groupRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;

    @Transactional
    public Long create(GroupRequest req, Member me) {
        return groupRepository.save(new InterviewGroup(req.name(), me)).getId();
    }

    public List<GroupResponse> getGroups() {
        return groupRepository.findAllWithQuestionCount();
    }

    @Transactional
    public void rename(Long groupId, GroupRequest req, Member me) {
        InterviewGroup group = find(groupId);
        checkOwnerOrAdmin(group.getCreatedBy(), me);
        group.rename(req.name());
    }

    @Transactional
    public void delete(Long groupId, Member me) {
        InterviewGroup group = find(groupId);
        checkOwnerOrAdmin(group.getCreatedBy(), me);
        if (answerRepository.existsByQuestionGroupId(groupId))
            throw new ResponseStatusException(CONFLICT, "답변이 달린 질문이 있는 그룹은 삭제할 수 없습니다.");
        questionRepository.deleteAllByGroupId(groupId);
        groupRepository.delete(group);
    }

    @Transactional
    public Long createQuestion(Long groupId, QuestionCreateRequest req, Member me) {
        InterviewGroup group = find(groupId);
        if (questionRepository.existsByGroupIdAndContent(groupId, req.content()))
            throw new ResponseStatusException(CONFLICT, "그룹에 이미 있는 질문입니다.");
        return questionRepository.save(
                new InterviewQuestion(group, me, req.content(), req.major(), req.category())).getId();
    }

    public List<QuestionResponse> getQuestions(Long groupId) {
        find(groupId);
        return questionRepository.findAllByGroupIdOrderByIdDesc(groupId).stream()
                .map(QuestionResponse::from).toList();
    }

    private InterviewGroup find(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "그룹이 없습니다."));
    }
}

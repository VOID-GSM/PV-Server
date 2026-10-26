package pv.domain.assignment.dto.response;

import pv.domain.assignment.dto.projection.SubmissionRow;
import pv.domain.assignment.dto.projection.TargetMemberRow;
import pv.domain.assignment.entity.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionStatusResponse(
        Long assignmentId,
        int total,
        long submittedCount,
        long notSubmittedCount,
        long lateCount,
        long passedCount,
        long resubmitRequestedCount,
        List<Item> items
) {
    // status == null && !submitted → 미제출
    public record Item(Long memberId, String memberName, boolean submitted, SubmissionStatus status,
                       boolean late, Long submissionId, LocalDateTime submittedAt) {
        public static Item of(TargetMemberRow t, SubmissionRow r) {
            if (r == null) {
                return new Item(t.id(), t.name(), false, null, false, null, null);
            }
            return new Item(t.id(), t.name(), true, r.status(), r.late(), r.id(), r.submittedAt());
        }
    }

    public static SubmissionStatusResponse of(Long assignmentId, List<Item> items) {
        long submitted = items.stream().filter(Item::submitted).count();
        return new SubmissionStatusResponse(
                assignmentId,
                items.size(),
                submitted,
                items.size() - submitted,
                items.stream().filter(Item::late).count(),
                items.stream().filter(i -> i.status() == SubmissionStatus.PASSED).count(),
                items.stream().filter(i -> i.status() == SubmissionStatus.RESUBMIT_REQUESTED).count(),
                items);
    }
}

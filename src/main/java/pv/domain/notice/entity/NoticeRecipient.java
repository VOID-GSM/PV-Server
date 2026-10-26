package pv.domain.notice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 공지 작성 시점의 대상자 목록. readAt이 null이면 미확인.
// row는 NoticeRecipientRepository.insertAll(네이티브 insert-select)로 한 번에 만든다.
// - 목록: (member_id, notice_id) 인덱스로 내가 받은 공지를 최신순 조회
// - 읽음 처리/현황/재알림: (notice_id, member_id) 유니크 인덱스 사용
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice_recipient",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notice_recipient_notice_member",
                columnNames = {"notice_id", "member_id"}),
        indexes = @Index(name = "idx_notice_recipient_member_notice", columnList = "member_id, notice_id"))
public class NoticeRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notice_id", nullable = false)
    private Long noticeId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public boolean isRead() {
        return readAt != null;
    }
}

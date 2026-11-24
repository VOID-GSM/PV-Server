-- [1단계] 배포 직후 실행 (여러 번 실행해도 안전)
INSERT INTO deadline_assignees (deadline_id, member_id)
SELECT id, assignee_id FROM deadline
WHERE assignee_id IS NOT NULL
    ON CONFLICT DO NOTHING;

-- [확인] 0이어야 함 (옮겨지지 않은 기존 담당자 수)
SELECT count(*) AS not_migrated
FROM deadline d
WHERE d.assignee_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM deadline_assignees a
                  WHERE a.deadline_id = d.id AND a.member_id = d.assignee_id);

-- [2단계] 이전 확인 후 다음 배포에서 (되돌릴 수 없음)
-- ALTER TABLE deadline DROP COLUMN assignee_id;
-- 면접 질문 그룹 도입 (PR #84) 데이터 이전
-- 대상 DB: PostgreSQL
--
-- 배경
--   interview_question에 group_id, created_by(NOT NULL)가 생겼고,
--   중복 기준이 "전체에서 content 유일" → "그룹 안에서 content 유일"로 바뀌었다.
--   ddl-auto=update는 기존 행을 채우지도, 예전 unique 제약을 지우지도 않는다.
--
-- 실행 순서: 배포 "전"에 [0]~[3] 실행 → 배포 → [확인]

-- [0] 값 확인: 팀 id와 부장 member id
SELECT m.id AS leader_id, m.team_id, m.name
FROM member m
         JOIN member_roles r ON r.member_id = m.id
WHERE r.role = 'LEADER';

-- [1] 그룹 테이블 + 컬럼 미리 만들기 (배포 시 Hibernate가 같은 이름이면 건너뜀)
CREATE TABLE IF NOT EXISTS interview_group (
                                               id         bigserial PRIMARY KEY,
                                               team_id    bigint       NOT NULL,
                                               name       varchar(50)  NOT NULL,
    created_by bigint       NOT NULL REFERENCES member (id),
    created_at timestamp(6)
    );
ALTER TABLE interview_question ADD COLUMN IF NOT EXISTS group_id   bigint;
ALTER TABLE interview_question ADD COLUMN IF NOT EXISTS created_by bigint;

-- [2] 기존 질문을 '기본' 그룹으로 (<TEAM_ID>, <LEADER_ID>를 [0] 결과로 바꿔서 실행)
INSERT INTO interview_group (team_id, name, created_by, created_at)
SELECT <TEAM_ID>, '기본', <LEADER_ID>, now()
WHERE NOT EXISTS (SELECT 1 FROM interview_group WHERE team_id = <TEAM_ID> AND name = '기본');

UPDATE interview_question
SET group_id   = (SELECT id FROM interview_group WHERE team_id = <TEAM_ID> AND name = '기본'),
    created_by = <LEADER_ID>
WHERE group_id IS NULL;

ALTER TABLE interview_question ALTER COLUMN group_id   SET NOT NULL;
ALTER TABLE interview_question ALTER COLUMN created_by SET NOT NULL;

-- [3] 예전 "content 전체 unique" 제약 삭제 (그룹이 다르면 같은 질문 허용)
--     아래로 이름을 확인한 뒤 DROP
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'interview_question'::regclass AND contype = 'u';
-- ALTER TABLE interview_question DROP CONSTRAINT <content만 들어간 unique 제약 이름>;

-- [확인] 0이어야 함
SELECT count(*) AS no_group FROM interview_question WHERE group_id IS NULL OR created_by IS NULL;

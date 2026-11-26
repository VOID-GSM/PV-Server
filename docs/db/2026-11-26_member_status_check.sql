-- MemberStatus에 LEFT(탈퇴) 추가 시 확인할 것
-- 대상 DB: PostgreSQL
--
-- Hibernate 6.2+는 @Enumerated(EnumType.STRING) 컬럼으로 테이블을 처음 만들 때
-- "check (status in ('PENDING','JOINED'))" 제약을 함께 만든다.
-- ddl-auto=update는 이 제약을 갱신하지 않으므로, 제약이 있으면 탈퇴 처리(LEFT 저장)가 실패한다.

-- [확인] 결과가 없으면 할 일 없음
SELECT conrelid::regclass AS table_name, conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE contype = 'c'
  AND conrelid = 'member'::regclass;

-- [조치] 위에서 status 관련 제약이 나오면 이름을 넣어 삭제
-- ALTER TABLE member DROP CONSTRAINT <conname>;

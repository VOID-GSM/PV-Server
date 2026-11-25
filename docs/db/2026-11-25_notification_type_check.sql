-- NotificationType에 DEADLINE_ASSIGNED 추가 시 확인할 것
-- 대상 DB: PostgreSQL
--
-- Hibernate 6.2+는 @Enumerated(EnumType.STRING) 컬럼으로 테이블을 처음 만들 때
-- "check (notification_type in (...))" 제약을 함께 만든다.
-- ddl-auto=update는 이 제약을 갱신하지 않으므로, 제약이 있으면 새 enum 값 저장이 실패한다.

-- [확인] 결과가 없으면 할 일 없음
SELECT conrelid::regclass AS table_name, conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE contype = 'c'
  AND conrelid IN ('notification'::regclass, 'notification_setting'::regclass);

-- [조치] 위에서 notification_type 관련 제약이 나오면 이름을 넣어 삭제
-- ALTER TABLE notification         DROP CONSTRAINT <conname>;
-- ALTER TABLE notification_setting DROP CONSTRAINT <conname>;

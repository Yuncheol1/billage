-- 회차 1건에 자동 확정(AUTO_CONFIRMED) 상환 이벤트가 2건 이상 생기지 않도록 막는 최후 방어선.
-- 중복은 회차 상태 선점(REQUESTED)에서 이미 막지만, 다른 경로가 생기더라도 DB가 한 번 더 거른다.
-- ddl-auto: update는 생성 컬럼·유니크 인덱스를 만들지 않으므로 수동 실행이 필요하다.
--
-- 같은 회차에 신고(REQUESTED)·반려(REJECTED) 이벤트는 여러 건일 수 있어 schedule_seq 단독 유니크는 쓸 수 없다.
-- MySQL은 부분 인덱스가 없으므로, AUTO_CONFIRMED일 때만 schedule_seq를 담고 나머지는 NULL인 생성 컬럼에
-- 유니크 인덱스를 건다. 유니크 인덱스는 NULL끼리 충돌하지 않는다.
--
-- 위반 시: 결과 반영 트랜잭션이 롤백되어 회차가 REQUESTED로 남고, 정산·운영 알림 경로로 넘어간다.

-- 1) 실행 전 확인. 결과가 있으면 인덱스 생성이 실패하므로 해당 회차를 먼저 직접 확인할 것.
SELECT schedule_seq, COUNT(*) AS cnt
FROM repayment_event
WHERE status = 'AUTO_CONFIRMED'
GROUP BY schedule_seq
HAVING COUNT(*) > 1;

-- 2) 생성 컬럼 + 유니크 인덱스
ALTER TABLE repayment_event
    ADD COLUMN auto_confirmed_schedule_seq INT
        GENERATED ALWAYS AS (IF(status = 'AUTO_CONFIRMED', schedule_seq, NULL)) STORED,
    ADD UNIQUE INDEX uk_repayment_event_auto_confirmed (auto_confirmed_schedule_seq);

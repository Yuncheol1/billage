-- 이체 시도 원장(transfer_attempt)을 회차 상태 선점 방식으로 대체
-- 이체 진행 여부는 repayment_schedule.status = 'REQUESTED'로, 요청 시각은 아래 컬럼으로 관리한다.
-- ddl-auto: update는 컬럼 추가만 하고 테이블 삭제는 하지 않으므로 수동으로 실행한다.

ALTER TABLE repayment_schedule
    ADD COLUMN transfer_requested_dttm DATETIME NULL;

-- transfer_attempt에 REQUESTED로 남은 행이 있다면, 삭제 전에 해당 회차를 직접 확인할 것.
-- SELECT * FROM transfer_attempt WHERE status = 'REQUESTED';
DROP TABLE IF EXISTS transfer_attempt;

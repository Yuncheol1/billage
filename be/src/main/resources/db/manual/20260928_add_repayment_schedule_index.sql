-- 자동이체 대상 조회용 인덱스.
-- ddl-auto: update는 이미 존재하는 테이블에 인덱스를 추가하지 않으므로 수동 실행이 필요하다.
-- 엔티티 선언: RepaymentSchedule @Table(indexes = ...)
--
-- 대상 쿼리 (RepaymentScheduleRepository.findDueScheduleSeqs)
--   WHERE rs.due_dt <= :today AND rs.status = 'NONPAID'
--
-- status(등치)가 선행, due_dt(범위)가 후행이어야 한다. 순서를 바꾸면 범위 조건 뒤의 컬럼을 타지 못한다.

ALTER TABLE repayment_schedule
ADD INDEX idx_schedule_status_due_dt (status, due_dt);

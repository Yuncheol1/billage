-- 회차 상태 변경 이력. 추가만 하고 고치거나 지우지 않는다.
-- 엔티티 선언: RepaymentScheduleHistory
-- ddl-auto: update면 새 테이블이라 자동으로 생성된다. 자동 생성을 끈 환경에서만 수동 실행한다.

CREATE TABLE IF NOT EXISTS repayment_schedule_history (
    history_seq         BIGINT       NOT NULL AUTO_INCREMENT,
    schedule_seq        INT          NOT NULL,
    from_status         VARCHAR(20)  NULL,
    to_status           VARCHAR(20)  NOT NULL,
    reason              VARCHAR(40)  NOT NULL,
    changed_by_user_seq INT          NULL,
    detail              VARCHAR(500) NULL,
    created_dttm        DATETIME(6)  NOT NULL,
    PRIMARY KEY (history_seq),
    INDEX idx_schedule_history_schedule (schedule_seq, history_seq),
    CONSTRAINT fk_schedule_history_schedule FOREIGN KEY (schedule_seq) REFERENCES repayment_schedule (schedule_seq),
    CONSTRAINT fk_schedule_history_user FOREIGN KEY (changed_by_user_seq) REFERENCES users (user_seq)
);

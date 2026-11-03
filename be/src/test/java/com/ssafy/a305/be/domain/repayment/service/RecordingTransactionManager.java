package com.ssafy.a305.be.domain.repayment.service;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 트랜잭션 시작·커밋·롤백 순서를 기록하는 테스트용 매니저
 * API 호출이 트랜잭션 밖에서 일어나는지 확인할 때 쓴다.
 */
class RecordingTransactionManager extends AbstractPlatformTransactionManager {

    final List<String> events = new ArrayList<>();

    @Override
    protected Object doGetTransaction() {
        return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        events.add("BEGIN");
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        events.add("COMMIT");
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        events.add("ROLLBACK");
    }
}

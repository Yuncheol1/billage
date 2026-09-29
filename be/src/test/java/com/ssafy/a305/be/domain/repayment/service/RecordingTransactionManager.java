package com.ssafy.a305.be.domain.repayment.service;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 트랜잭션의 시작·커밋·롤백 시점을 기록하는 테스트용 매니저.
 *
 * <p>DB 없이 "외부 호출이 트랜잭션 밖에서 일어나는가", "시도 기록이 호출보다 먼저 커밋되는가"
 * 같은 <b>트랜잭션 경계</b>를 검증하기 위한 것이다. 기록은 {@link #events}에 이벤트 이름으로
 * 남으며, 테스트가 같은 목록에 {@code "API"} 같은 표식을 함께 넣어 순서를 비교한다.</p>
 *
 * <p>{@link AbstractPlatformTransactionManager}를 상속하므로 동기화(synchronization)가
 * 실제 매니저와 똑같이 켜지고 꺼진다. 그래서
 * {@code TransactionSynchronizationManager.isActualTransactionActive()}가 실제와 같은 값을 준다.</p>
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

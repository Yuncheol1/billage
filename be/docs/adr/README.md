# 설계 결정 기록

자동이체와 계약에서 내린 결정을 한 장씩 남깁니다. 각 문서는 상황, 검토한 선택지, 결정, 감수한 것 순서입니다.

| 번호 | 결정 | 
|---|---|
| [001](001-conditional-update-claim.md) | 이중 출금은 락 대신 회차 상태의 조건부 UPDATE로 막는다 |
| [002](002-transfer-outside-transaction.md) | 이체 API는 DB 트랜잭션 밖에서 호출한다 | 
| [003](003-no-retry-reconcile.md) | 결과를 모르는 이체는 다시 보내지 않고 거래내역으로 확정한다 | 
| [004](004-due-date-lte.md) | 이체 대상은 기한 "당일"이 아니라 "오늘 이하"로 조회한다 | 
| [005](005-clause-pessimistic-lock.md) | 특약 순번은 계약 행 비관적 락으로 정한다 |  
| [006](006-schedule-index.md) | 이체 대상 조회 인덱스는 (status, due_dt) 순서로 건다 | 


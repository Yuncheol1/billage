package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;

/**
 * 상환 이체의 적요(거래 요약) 문자열을 만들고 되찾는 유틸.
 *
 * <p>SSAFY 이체 API는 기관거래고유번호를 매 호출마다 새로 발급하므로 은행 측 멱등성이
 * 없다. 따라서 타임아웃처럼 결과를 알 수 없는 호출 이후 "돈이 실제로 나갔는가"를 판정할
 * 수단은 <b>거래내역 조회뿐</b>이고, 그러려면 우리가 보낸 이체를 거래내역에서 다시
 * 찾아낼 수 있어야 한다. 적요는 우리가 자유롭게 채울 수 있는 유일한 칸이므로 여기에
 * 회차 식별자를 심는다.</p>
 *
 * <p>형식: {@code BL{scheduleSeq}-{attemptNo} {installmentNo}회차 {contractId}}<br>
 * 예: {@code BL12345-1 3회차 C-20260918-3F9A0C}</p>
 *
 * <p><b>마커가 맨 앞에 오는 이유</b>: SSAFY의 적요 길이 상한을 알 수 없어(초과 시
 * {@code A1018}) 은행이 뒤를 자를 가능성을 배제할 수 없다. 뒤가 잘리면 가독성만 나빠지지만
 * 마커가 잘리면 정산이 불가능해져 돈 문제가 된다. 비대칭적이므로 마커 생존을 우선한다.</p>
 *
 * <p>생성({@link #of})과 판정({@link #matches})을 한 곳에 두어 두 포맷이 갈라지지 않게 한다.
 * 후속 정산 작업은 {@link #matches}를 그대로 사용한다.</p>
 */
final class RepaymentTransferMemo {

    private static final String MARKER_PREFIX = "BL";

    /**
     * 마커와 시도 번호를 가르는 구분자.
     *
     * <p>이 구분자가 없으면 {@code BL123}이 {@code BL1234}의 접두사가 되어 서로 다른 회차를
     * 같은 것으로 오판한다. 판정이 접두사 비교인 이상 구분자는 생략할 수 없다.</p>
     */
    private static final String ATTEMPT_DELIMITER = "-";

    /**
     * 적요 최대 길이(방어적 상한).
     *
     * <p>정상 형식은 40자 안팎이라 평소에는 걸리지 않는다. contractId 형식이 바뀌는 등의
     * 이유로 예상보다 길어졌을 때 {@code A1018}로 이체가 통째로 거절되는 것보다는, 뒤를 잘라
     * 내고 이체를 성사시키는 편이 낫다. 마커가 맨 앞이므로 잘려도 정산 키는 남는다.</p>
     */
    private static final int MAX_LENGTH = 60;

    private RepaymentTransferMemo() {
    }

    /**
     * 시도 번호를 추적하기 전까지 쓰는 적요.
     *
     * <p>아직 {@code TransferAttempt} 테이블이 없어 실제 시도 횟수를 알 수 없으므로 1로 고정한다.
     * 형식 자체는 최종형이므로, 시도 추적이 도입되면 {@link #of(RepaymentSchedule, int)}로
     * 바꿔 부르기만 하면 되고 {@link #matches} 쪽은 손대지 않아도 된다.</p>
     */
    static String of(RepaymentSchedule schedule) {
        return of(schedule, 1);
    }

    /**
     * 상환 회차 1건의 이체 적요를 만든다.
     *
     * @param schedule  대상 회차. 계약 연관이 초기화되어 있어야 한다.
     * @param attemptNo 이 회차에 대한 몇 번째 이체 시도인지 (1부터)
     */
    static String of(RepaymentSchedule schedule, int attemptNo) {
        StringBuilder memo = new StringBuilder(markerOf(schedule.getScheduleSeq(), attemptNo));

        // installmentNo는 nullable이다. 사람이 읽을 때의 도움말일 뿐이므로 없으면 생략한다.
        Integer installmentNo = schedule.getInstallmentNo();
        if (installmentNo != null) {
            memo.append(' ').append(installmentNo).append("회차");
        }

        Contract contract = schedule.getContract();
        String contractId = contract != null ? contract.getContractId() : null;
        if (contractId != null && !contractId.isBlank()) {
            memo.append(' ').append(contractId);
        }

        if (memo.length() > MAX_LENGTH) {
            memo.setLength(MAX_LENGTH);
        }
        return memo.toString();
    }

    /** 정산이 거래내역에서 찾아낼 식별자. 적요의 맨 앞에 놓인다. */
    static String markerOf(Integer scheduleSeq, int attemptNo) {
        return MARKER_PREFIX + scheduleSeq + ATTEMPT_DELIMITER + attemptNo;
    }

    /**
     * 거래내역 한 건이 해당 회차의 이체인지 판정한다.
     *
     * <p>시도 번호는 보지 않는다. 정산이 답해야 하는 질문은 "이 회차의 이체가 나갔는가"이지
     * "몇 번째 시도가 나갔는가"가 아니기 때문이다. 시도 번호는 사후 진단용으로만 남긴다.</p>
     *
     * <p>접두사가 아니라 포함 여부로 판정하는 이유: 은행이 적요 앞에 자체 문구를 덧붙일 수
     * 있다(이 코드베이스에도 {@code "(수시입출금) : 입금"} 같은 SSAFY 측 형식이 관찰된다).
     * 구분자 덕분에 {@code "BL12-"}가 {@code "BL121-2"}의 부분 문자열이 되지 않으므로
     * 포함 판정으로도 회차 간 오인은 생기지 않는다.</p>
     *
     * @param transactionSummary 거래내역의 적요. null이면 false.
     */
    static boolean matches(String transactionSummary, Integer scheduleSeq) {
        if (transactionSummary == null || scheduleSeq == null) {
            return false;
        }
        return transactionSummary.contains(MARKER_PREFIX + scheduleSeq + ATTEMPT_DELIMITER);
    }
}

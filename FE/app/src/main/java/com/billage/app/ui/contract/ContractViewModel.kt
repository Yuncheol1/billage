package com.billage.app.ui.contract

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ─────────────────────────────────────────
// 상환 주기 선택지
// ─────────────────────────────────────────
enum class RepaymentMethod {
    LUMP_SUM,    // 일시상환
    MONTHLY,     // 월별
    QUARTERLY,   // 분기별 (3개월)
    SEMI_ANNUAL, // 반기별 (6개월)
    ANNUAL       // 연별
}

// ─────────────────────────────────────────
// 상환일 유형 선택지
// LUMP_SUM이 아닐 때만 사용
// ─────────────────────────────────────────
enum class RepaymentDayType {
    FIRST,   // 매월 1일
    LAST,    // 매월 말일
    CUSTOM   // 직접 지정 (1~28일)
}

// ─────────────────────────────────────────
// 8단계 입력값을 하나로 묶은 데이터 클래스
// 각 필드가 null 이면 아직 입력 안 된 상태
// ─────────────────────────────────────────
data class ContractFormState(
    val isFamily: Boolean? = null,                    // Step 1: 가족 여부
    val amount: String = "",                           // Step 2: 금액
    val interestRate: String = "",                     // Step 3: 이자율
    val startDate: String = "",                        // Step 4: 시작일 (yyyy-MM-dd)
    val endDate: String = "",                          // Step 5: 만기일 (yyyy-MM-dd)
    val repaymentMethod: RepaymentMethod? = null,      // Step 6: 상환 주기
    val repaymentDayType: RepaymentDayType? = null,    // Step 6: 상환일 유형
    val repaymentCustomDay: Int? = null,               // Step 6: 직접 지정일 (1~28)
    val debtorPhone: String = "",                      // Step 7: 채무자 연락처
)

// ─────────────────────────────────────────
// ViewModel
// 8단계 입력값 + 현재 스텝 번호를 관리한다
// ─────────────────────────────────────────
class ContractViewModel : ViewModel() {

    // 현재 스텝 (1 ~ 8)
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    // 전체 입력값 상태
    private val _formState = MutableStateFlow(ContractFormState())
    val formState: StateFlow<ContractFormState> = _formState.asStateFlow()

    // ── 스텝 이동 ──────────────────────────

    fun nextStep() {
        if (_currentStep.value < TOTAL_STEPS) {
            _currentStep.update { it + 1 }
        }
    }

    fun prevStep() {
        if (_currentStep.value > 1) {
            _currentStep.update { it - 1 }
        }
    }

    fun goToStep(step: Int) {
        if (step in 1..TOTAL_STEPS) {
            _currentStep.update { step }
        }
    }

    // ── 각 스텝 입력값 업데이트 ────────────

    // Step 1
    fun updateIsFamily(isFamily: Boolean) {
        _formState.update { it.copy(isFamily = isFamily) }
    }

    // Step 2
    fun updateAmount(amount: String) {
        _formState.update { it.copy(amount = amount) }
    }

    // Step 3
    fun updateInterestRate(rate: String) {
        _formState.update { it.copy(interestRate = rate) }
    }

    // Step 4
    fun updateStartDate(date: String) {
        _formState.update { it.copy(startDate = date) }
    }

    // Step 5
    fun updateEndDate(date: String) {
        _formState.update { it.copy(endDate = date) }
    }

    // Step 6
    fun updateRepaymentMethod(method: RepaymentMethod) {
        // 상환 주기가 바뀌면 상환일 유형도 초기화
        _formState.update {
            it.copy(
                repaymentMethod = method,
                repaymentDayType = null,
                repaymentCustomDay = null
            )
        }
    }

    fun updateRepaymentDayType(dayType: RepaymentDayType) {
        // 상환일 유형이 바뀌면 직접 지정일 초기화
        _formState.update {
            it.copy(
                repaymentDayType = dayType,
                repaymentCustomDay = null
            )
        }
    }

    fun updateRepaymentCustomDay(day: Int) {
        _formState.update { it.copy(repaymentCustomDay = day) }
    }

    // Step 7
    fun updateDebtorPhone(phone: String) {
        _formState.update { it.copy(debtorPhone = phone) }
    }

    // ── 각 스텝 다음 버튼 활성화 여부 ──────
    fun isNextEnabled(): Boolean {
        return when (_currentStep.value) {
            1 -> _formState.value.isFamily != null
            2 -> _formState.value.amount.isNotBlank()
            3 -> _formState.value.interestRate.isNotBlank()
            4 -> _formState.value.startDate.isNotBlank()
            5 -> _formState.value.endDate.isNotBlank()
            6 -> isRepaymentStepComplete()
            7 -> isValidPhoneNumber(_formState.value.debtorPhone)
            else -> true // Step 8 미리보기는 항상 활성화
        }
    }

    // Step 6 완료 조건
    // - 일시상환: 주기만 선택하면 완료
    // - 그 외: 주기 + 상환일 유형 선택 필요
    //          상환일 유형이 CUSTOM이면 날짜까지 입력 필요
    private fun isRepaymentStepComplete(): Boolean {
        val method = _formState.value.repaymentMethod ?: return false
        if (method == RepaymentMethod.LUMP_SUM) return true

        val dayType = _formState.value.repaymentDayType ?: return false
        if (dayType == RepaymentDayType.CUSTOM) {
            return _formState.value.repaymentCustomDay != null
        }
        return true
    }

    // Step 7 전화번호 형식 검증
    // 010-XXXX-XXXX 또는 01012345678 형태 모두 허용
    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.replace("-", "").matches(Regex("^010\\d{8}$"))
    }

    companion object {
        const val TOTAL_STEPS = 8
    }
}
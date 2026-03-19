package com.billage.app.ui.contract

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ContractRole {
    CREDITOR, // 빌려주기 (채권자)
    DEBTOR    // 빌리기 (채무자)
}

enum class RepaymentMethod {
    LUMP_SUM,
    MONTHLY,
    QUARTERLY,
    SEMI_ANNUAL,
    ANNUAL
}

data class ContractFormState(
    val role: ContractRole = ContractRole.CREDITOR,
    val debtorUserSeq: Int? = null,
    val debtorName: String = "",
    val debtorPhone: String = "",
    val isFamily: Boolean? = null,
    val amount: String = "",
    val interestRate: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val repaymentMethod: RepaymentMethod? = null,
)

class ContractViewModel : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _formState = MutableStateFlow(ContractFormState())
    val formState: StateFlow<ContractFormState> = _formState.asStateFlow()

    fun nextStep() {
        if (_currentStep.value < TOTAL_STEPS) _currentStep.update { it + 1 }
    }

    fun prevStep() {
        if (_currentStep.value > 1) _currentStep.update { it - 1 }
    }

    fun goToStep(step: Int) {
        if (step in 1..TOTAL_STEPS) _currentStep.update { step }
    }

    fun updateRole(role: ContractRole) {
        _formState.update { it.copy(role = role) }
    }

    fun updateDebtorUser(userSeq: Int, name: String, phone: String) {
        _formState.update {
            it.copy(
                debtorUserSeq = userSeq,
                debtorName = name,
                debtorPhone = phone
            )
        }
    }

    fun updateDebtorName(name: String) {
        _formState.update { it.copy(debtorName = name) }
    }

    fun updateDebtorPhone(phone: String) {
        _formState.update { it.copy(debtorPhone = phone) }
    }

    fun updateIsFamily(isFamily: Boolean) {
        _formState.update { it.copy(isFamily = isFamily) }
    }

    fun updateAmount(amount: String) {
        _formState.update { it.copy(amount = amount) }
    }

    fun updateInterestRate(rate: String) {
        _formState.update { it.copy(interestRate = rate) }
    }

    // 날짜 변경 시 상환 방식 초기화 — 기간이 바뀌면 이전 선택이 유효하지 않을 수 있음
    fun updateStartDate(date: String) {
        _formState.update { it.copy(startDate = date, repaymentMethod = null) }
    }

    fun updateEndDate(date: String) {
        _formState.update { it.copy(endDate = date, repaymentMethod = null) }
    }

    fun updateRepaymentMethod(method: RepaymentMethod) {
        _formState.update { it.copy(repaymentMethod = method) }
    }

    // 총 상환 횟수 계산 — 1달 미만 잔여분은 제외 (내림 처리)
    fun calculateTotalInstallments(): Int? {
        val method = _formState.value.repaymentMethod ?: return null
        if (method == RepaymentMethod.LUMP_SUM) return null

        val start = runCatching { LocalDate.parse(_formState.value.startDate) }.getOrNull() ?: return null
        val end = runCatching { LocalDate.parse(_formState.value.endDate) }.getOrNull() ?: return null
        val months = ChronoUnit.MONTHS.between(start, end).toInt()

        return when (method) {
            RepaymentMethod.MONTHLY -> months
            RepaymentMethod.QUARTERLY -> months / 3
            RepaymentMethod.SEMI_ANNUAL -> months / 6
            RepaymentMethod.ANNUAL -> months / 12
            else -> null
        }
    }

    // 상환일 = 시작일의 일(day)로 자동 고정
    fun calculateRepaymentDay(): Int {
        return runCatching {
            LocalDate.parse(_formState.value.startDate).dayOfMonth
        }.getOrDefault(1)
    }

    fun isNextEnabled(): Boolean {
        return when (_currentStep.value) {
            1 -> _formState.value.debtorUserSeq != null
            2 -> _formState.value.isFamily != null
            3 -> _formState.value.amount.isNotBlank()
            4 -> _formState.value.interestRate.isNotBlank()
            5 -> _formState.value.startDate.isNotBlank() && _formState.value.endDate.isNotBlank()
            6 -> _formState.value.repaymentMethod != null
            else -> true
        }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.replace("-", "").matches(Regex("^010\\d{8}$"))
    }

    companion object {
        const val TOTAL_STEPS = 6
    }
}
package com.billage.app.ui.contract

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class RepaymentMethod {
    LUMP_SUM,
    MONTHLY,
    QUARTERLY,
    SEMI_ANNUAL,
    ANNUAL
}

enum class RepaymentDayType {
    FIRST,
    LAST,
    CUSTOM
}

data class ContractFormState(
    val isFamily: Boolean? = null,
    val amount: String = "",
    val interestRate: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val repaymentMethod: RepaymentMethod? = null,
    val repaymentDayType: RepaymentDayType? = null,
    val repaymentCustomDay: Int? = null,
    val debtorName: String = "",
    val debtorPhone: String = "",
    val specialTerms: List<String> = emptyList(), // Step 7: 선택된 특약 목록
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

    fun updateIsFamily(isFamily: Boolean) {
        _formState.update { it.copy(isFamily = isFamily) }
    }

    fun updateAmount(amount: String) {
        _formState.update { it.copy(amount = amount) }
    }

    fun updateInterestRate(rate: String) {
        _formState.update { it.copy(interestRate = rate) }
    }

    fun updateStartDate(date: String) {
        _formState.update {
            it.copy(
                startDate = date,
                repaymentMethod = null,
                repaymentDayType = null,
                repaymentCustomDay = null
            )
        }
    }

    fun updateEndDate(date: String) {
        _formState.update {
            it.copy(
                endDate = date,
                repaymentMethod = null,
                repaymentDayType = null,
                repaymentCustomDay = null
            )
        }
    }

    fun updateRepaymentMethod(method: RepaymentMethod) {
        _formState.update {
            it.copy(
                repaymentMethod = method,
                repaymentDayType = null,
                repaymentCustomDay = null
            )
        }
    }

    fun updateRepaymentDayType(dayType: RepaymentDayType) {
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

    fun updateDebtorName(name: String) {
        _formState.update { it.copy(debtorName = name) }
    }

    fun updateDebtorPhone(phone: String) {
        _formState.update { it.copy(debtorPhone = phone) }
    }

    fun addSpecialTerm(term: String) {
        if (term.isNotBlank() && !_formState.value.specialTerms.contains(term)) {
            _formState.update { it.copy(specialTerms = it.specialTerms + term) }
        }
    }

    fun removeSpecialTerm(term: String) {
        _formState.update { it.copy(specialTerms = it.specialTerms - term) }
    }

    fun isNextEnabled(): Boolean {
        return when (_currentStep.value) {
            1 -> _formState.value.isFamily != null
            2 -> _formState.value.amount.isNotBlank()
            3 -> _formState.value.interestRate.isNotBlank()
            4 -> _formState.value.startDate.isNotBlank() && _formState.value.endDate.isNotBlank()
            5 -> isRepaymentStepComplete()
            6 -> _formState.value.debtorName.isNotBlank()
                    && isValidPhoneNumber(_formState.value.debtorPhone)
            7 -> true // 특약은 선택사항
            else -> true
        }
    }

    private fun isRepaymentStepComplete(): Boolean {
        val method = _formState.value.repaymentMethod ?: return false
        if (method == RepaymentMethod.LUMP_SUM) return true
        val dayType = _formState.value.repaymentDayType ?: return false
        if (dayType == RepaymentDayType.CUSTOM) {
            return _formState.value.repaymentCustomDay != null
        }
        return true
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.replace("-", "").matches(Regex("^010\\d{8}$"))
    }

    companion object {
        const val TOTAL_STEPS = 7
    }
}
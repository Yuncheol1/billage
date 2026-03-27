package com.billage.app.ui.contract.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.local.AuthSessionStorage
import com.billage.app.data.remote.api.ChatApi
import com.billage.app.data.remote.api.ContractApi
import com.billage.app.data.remote.api.CreateContractRequest
import com.billage.app.data.remote.api.UserApi
import com.billage.app.data.remote.dto.UserLookupRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class ContractRole { CREDITOR, DEBTOR }

enum class RepaymentMethod {
    LUMP_SUM, MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL
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

// 상대방 검색 결과 상태
sealed class UserSearchState {
    object Idle : UserSearchState()           // 검색 전
    object Loading : UserSearchState()        // 검색 중
    data class Found(                         // 등록된 사용자 발견
        val userSeq: Int,
        val name: String,
        val phone: String,
        val profileImageUrl: String? = null
    ) : UserSearchState()
    object NotFound : UserSearchState()       // 미등록 (초대 필요)
    data class Error(val message: String) : UserSearchState()
}

sealed class ContractSubmitState {
    object Idle : ContractSubmitState()
    object Loading : ContractSubmitState()
    data class Success(
        val contractSeq: Int,
        val contractId: String,
        val roomId: String,
        val opponentName: String
    ) : ContractSubmitState()
    data class Error(val message: String) : ContractSubmitState()
}

@HiltViewModel
class ContractCreateViewModel @Inject constructor(
    private val contractApi: ContractApi,
    private val userApi: UserApi,
    private val chatApi: ChatApi,
    private val authSessionStorage: AuthSessionStorage
) : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _formState = MutableStateFlow(ContractFormState())
    val formState: StateFlow<ContractFormState> = _formState.asStateFlow()

    private val _submitState = MutableStateFlow<ContractSubmitState>(ContractSubmitState.Idle)
    val submitState: StateFlow<ContractSubmitState> = _submitState.asStateFlow()

    // 상대방 검색 상태
    private val _searchState = MutableStateFlow<UserSearchState>(UserSearchState.Idle)
    val searchState: StateFlow<UserSearchState> = _searchState.asStateFlow()

    // ─────────────────────────────────────────
    // 단계 이동
    // ─────────────────────────────────────────

    fun nextStep() {
        if (_currentStep.value < TOTAL_STEPS) _currentStep.update { it + 1 }
    }

    fun prevStep() {
        if (_currentStep.value > 1) _currentStep.update { it - 1 }
    }

    fun goToStep(step: Int) {
        if (step in 1..TOTAL_STEPS) _currentStep.update { step }
    }

    // ─────────────────────────────────────────
    // 폼 상태 업데이트
    // ─────────────────────────────────────────

    fun updateRole(role: ContractRole) {
        _formState.update { it.copy(role = role) }
    }

    fun updateDebtorName(name: String) {
        _formState.update { it.copy(debtorName = name, debtorUserSeq = null) }
        _searchState.value = UserSearchState.Idle  // 이름 바뀌면 검색 결과 초기화
    }

    fun updateDebtorPhone(phone: String) {
        _formState.update { it.copy(debtorPhone = phone, debtorUserSeq = null) }
        _searchState.value = UserSearchState.Idle  // 번호 바뀌면 검색 결과 초기화
    }

    fun updateDebtorUser(userSeq: Int, name: String, phone: String) {
        _formState.update { it.copy(debtorUserSeq = userSeq, debtorName = name, debtorPhone = phone) }
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
        _formState.update { it.copy(startDate = date, repaymentMethod = null) }
    }

    fun updateEndDate(date: String) {
        _formState.update { it.copy(endDate = date, repaymentMethod = null) }
    }

    fun updateRepaymentMethod(method: RepaymentMethod) {
        _formState.update { it.copy(repaymentMethod = method) }
    }

    // ─────────────────────────────────────────
    // 상대방 검색 API 호출
    // ─────────────────────────────────────────

    fun searchUser() {
        val name = _formState.value.debtorName.trim()
        val phone = _formState.value.debtorPhone  // 숫자만, 예: "01012345678"

        if (name.isBlank() || !isValidPhoneNumber(phone)) return

        viewModelScope.launch {
            _searchState.value = UserSearchState.Loading
            try {
                val response = userApi.lookupByPhone(
                    UserLookupRequest(realName = name, phone = phone)
                )
                val data = response.data
                if (data != null && data.registered && data.userSeq != null) {
                    // 자기 자신과 계약 시도 차단
                    if (data.userSeq == authSessionStorage.getUserSeq()) {
                        _formState.update { it.copy(debtorUserSeq = null) }
                        _searchState.value = UserSearchState.Error("자기 자신과는 계약할 수 없어요")
                        return@launch
                    }
                    // 등록된 사용자 → 자동으로 선택 처리
                    updateDebtorUser(data.userSeq, data.realName ?: name, phone)
                    _searchState.value = UserSearchState.Found(
                        userSeq = data.userSeq,
                        name = data.realName ?: name,
                        phone = phone,
                        profileImageUrl = data.profileImageUrl
                    )
                } else {
                    // 미등록 → 선택 초기화
                    _formState.update { it.copy(debtorUserSeq = null) }
                    _searchState.value = UserSearchState.NotFound
                }
            } catch (e: Exception) {
                _formState.update { it.copy(debtorUserSeq = null) }
                _searchState.value = UserSearchState.Error("검색 중 오류가 발생했어요. 다시 시도해 주세요.")
            }
        }
    }

    // ─────────────────────────────────────────
    // 다음 버튼 활성화 조건
    // ─────────────────────────────────────────

    fun isNextEnabled(): Boolean {
        return when (_currentStep.value) {
            1 -> _formState.value.debtorUserSeq != null  // 검색으로 선택된 사용자 있어야 함
            2 -> _formState.value.isFamily != null
            3 -> _formState.value.amount.isNotBlank()
            4 -> _formState.value.interestRate.isNotBlank()
            5 -> _formState.value.startDate.isNotBlank() && _formState.value.endDate.isNotBlank()
            6 -> _formState.value.repaymentMethod != null
            else -> true
        }
    }

    // ─────────────────────────────────────────
    // 계약 초안 제출
    // ─────────────────────────────────────────

    fun submitContractDraft() {
        val myUserSeq = authSessionStorage.getUserSeq()
        if (myUserSeq == -1) {
            _submitState.value = ContractSubmitState.Error("로그인 정보를 찾을 수 없어요. 다시 로그인해 주세요.")
            return
        }

        val form = _formState.value

        val counterpartySeq = form.debtorUserSeq ?: run {
            _submitState.value = ContractSubmitState.Error("상대방 정보가 없어요.")
            return
        }

        // 역할에 따라 채권자/채무자 결정
        // TODO: [BE 수정 필요] 빌리기(DEBTOR) 역할로 계약 생성 시 서버에서 "본인과의 계약은 불가합니다" 오류 발생.
        //  현재 서버가 creditorUserSeq를 JWT 사용자로 강제 설정하는 것으로 추정됨.
        //  백엔드에서 채무자(debtorUserSeq == JWT.userSeq) 개시 계약을 지원하도록 수정 후
        //  아래 로직이 정상 동작하는지 재확인 필요.
        val creditorUserSeq = if (form.role == ContractRole.CREDITOR) myUserSeq else counterpartySeq
        val debtorSeq       = if (form.role == ContractRole.CREDITOR) counterpartySeq else myUserSeq
        val isFamily = form.isFamily ?: run {
            _submitState.value = ContractSubmitState.Error("가족 여부를 선택해 주세요.")
            return
        }
        val principalAmount = form.amount.toLongOrNull() ?: run {
            _submitState.value = ContractSubmitState.Error("금액을 확인해 주세요.")
            return
        }
        val interestRate = form.interestRate.toDoubleOrNull()?.div(100) ?: run {
            _submitState.value = ContractSubmitState.Error("이자율을 확인해 주세요.")
            return
        }
        val repaymentMethod = form.repaymentMethod ?: run {
            _submitState.value = ContractSubmitState.Error("상환 방식을 선택해 주세요.")
            return
        }

        val request = CreateContractRequest(
            family = isFamily,
            creditorUserSeq = creditorUserSeq,
            debtorUserSeq = debtorSeq,
            principalAmount = principalAmount,
            interestRate = interestRate,
            repaymentMethod = repaymentMethod.name,
            totalInstallments = calculateTotalInstallments(),
            repaymentDay = calculateRepaymentDay(),
            startDt = form.startDate,
            endDt = form.endDate
        )

        viewModelScope.launch {
            _submitState.value = ContractSubmitState.Loading
            try {
                val response = contractApi.createContractDraft(request)
                val data = response.data
                if (response.status == 201 && data != null) {
                    // 계약 생성 성공 후 채팅방 생성/조회 시도
                    val chatRoom = chatApi.createRoomByUserSeq(
                        userSeq = creditorUserSeq,
                        targetUserSeq = debtorSeq
                    )
                    
                    _submitState.value = ContractSubmitState.Success(
                        contractSeq = data.contractSeq,
                        contractId = data.contractId,
                        roomId = chatRoom.id,
                        opponentName = form.debtorName
                    )
                } else {
                    _submitState.value = ContractSubmitState.Error(response.message)
                }
            } catch (e: Exception) {
                _submitState.value = ContractSubmitState.Error("계약 또는 채팅방 생성에 실패했어요. 다시 시도해 주세요.")
            }
        }
    }

    // ─────────────────────────────────────────
    // 상태 초기화
    // ─────────────────────────────────────────

    fun resetSubmitState() {
        _submitState.value = ContractSubmitState.Idle
    }

    fun resetForm(role: ContractRole = ContractRole.CREDITOR, initialName: String? = null) {
        _currentStep.value = 1
        _formState.value = ContractFormState(
            role = role,
            debtorName = initialName ?: ""
        )
        _submitState.value = ContractSubmitState.Idle
        _searchState.value = UserSearchState.Idle
    }

    // ─────────────────────────────────────────
    // 계산 함수
    // ─────────────────────────────────────────

    fun calculateTotalInstallments(): Int? {
        val method = _formState.value.repaymentMethod ?: return null
        
        val start = runCatching { LocalDate.parse(_formState.value.startDate) }.getOrNull() ?: return null
        val end = runCatching { LocalDate.parse(_formState.value.endDate) }.getOrNull() ?: return null
        
        // 개월 수 차이 계산
        val monthsBetween = ChronoUnit.MONTHS.between(start, end).toInt()
        
        // 종료일의 일자가 시작일의 일자보다 작으면(예: 1/15 ~ 3/10) 
        // ChronoUnit.MONTHS는 1개월로 계산되지만, 이자 지급을 위해 2개월로 올림 처리
        val totalMonths = if (end.dayOfMonth < start.dayOfMonth) {
            monthsBetween + 1
        } else {
            monthsBetween
        }
        
        // 최소 1회 보장
        val finalInstallments = if (totalMonths <= 0) 1 else totalMonths

        return when (method) {
            RepaymentMethod.LUMP_SUM -> finalInstallments
            RepaymentMethod.MONTHLY -> finalInstallments
            RepaymentMethod.QUARTERLY -> (finalInstallments / 3).coerceAtLeast(1)
            RepaymentMethod.SEMI_ANNUAL -> (finalInstallments / 6).coerceAtLeast(1)
            RepaymentMethod.ANNUAL -> (finalInstallments / 12).coerceAtLeast(1)
            else -> null
        }
    }

    fun calculateRepaymentDay(): Int {
        return runCatching {
            LocalDate.parse(_formState.value.startDate).dayOfMonth
        }.getOrDefault(1)
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.matches(Regex("^010\\d{8}$"))
    }

    companion object {
        const val TOTAL_STEPS = 6
    }
}

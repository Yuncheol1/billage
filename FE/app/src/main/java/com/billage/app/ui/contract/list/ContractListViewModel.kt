package com.billage.app.ui.contract.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.core.events.AppEvent
import com.billage.app.core.events.AppEventBus
import com.billage.app.data.remote.api.ContractApi
import com.billage.app.data.remote.api.ContractListResponse
import com.billage.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// ─────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────

data class ContractListUiState(
    val isLoading: Boolean = false,
    val contracts: List<ContractCardModel> = emptyList(),
    val error: String? = null,
    val deleteError: String? = null,
    val hasNotification: Boolean = false
)

// ─────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────

@HiltViewModel
class ContractListViewModel @Inject constructor(
    private val contractApi: ContractApi,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractListUiState())
    val uiState: StateFlow<ContractListUiState> = _uiState.asStateFlow()
    private var currentRole: String? = "ALL"
    private var currentStatus: String? = null

    init {
        observeUnreadCount()
        observeAppEvents()
        refreshUnreadCount()
        fetchContracts()
    }

    /**
     * 계약 취소/삭제 (DRAFT, NEGOTIATING 상태만 허용).
     * 성공 시 로컬 목록에서 즉시 제거.
     */
    fun deleteContract(contractSeq: Int) {
        viewModelScope.launch {
            try {
                contractApi.deleteContract(contractSeq)
                _uiState.update { it.copy(contracts = it.contracts.filter { c -> c.id != contractSeq }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(deleteError = "계약 삭제에 실패했습니다.") }
            }
        }
    }

    fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = null) }
    }

    /**
     * 계약 목록 조회.
     * [role]: null이면 "ALL" (전체), 있으면 "CREDITOR" / "DEBTOR"
     * [status]: null이면 모든 상태 조회, 있으면 특정 status
     */
    fun fetchContracts(
        role: String? = null,
        status: String? = null
    ) {
        currentRole = role ?: "ALL"
        currentStatus = status
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = contractApi.getContracts(
                    role = currentRole,
                    status = currentStatus
                )
                val mapped = response.data.map { it.toContractCardModel() }
                _uiState.update { it.copy(isLoading = false, contracts = mapped) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "계약 목록을 불러오지 못했습니다.")
                }
            }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            runCatching { notificationRepository.refreshUnreadCount() }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            notificationRepository.unreadCount.collectLatest { unreadCount ->
                _uiState.update { it.copy(hasNotification = unreadCount > 0) }
            }
        }
    }

    private fun observeAppEvents() {
        viewModelScope.launch {
            AppEventBus.events.collectLatest { event ->
                when (event) {
                    is AppEvent.ContractDeleted -> {
                        fetchContracts(currentRole, currentStatus)
                        refreshUnreadCount()
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Mapper: API Response → UI Model
// ─────────────────────────────────────────────────────────

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * API ContractListResponse → 화면 표시용 ContractCardModel 변환.
 * D-Day는 endDt 기준으로 프론트에서 직접 계산.
 */
private fun ContractListResponse.toContractCardModel(): ContractCardModel {
    val role = this.role?.toContractRoleFilter() ?: ContractRoleFilter.All
    val status = this.status?.toContractStatusFilter() ?: ContractStatusFilter.All

    // 금액 포맷 (예: 1000000 → "1,000,000원")
    val amountText = principalAmount
        ?.toLong()
        ?.let { "%,d원".format(it) }
        ?: "-"

    // 만기일 + D-Day 계산
    val endDate = runCatching { LocalDate.parse(this.endDt, dateFormatter) }.getOrNull()
    val today = LocalDate.now()
    val dDayValue = endDate?.let { ChronoUnit.DAYS.between(today, it) }

    val dueDateText = endDate?.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")) ?: "-"

    val (dDayLabel, dDayColorHex) = when {
        dDayValue == null -> Pair("-", 0xFF98A2B3.toInt())
        dDayValue < 0 -> Pair("D+${-dDayValue}", 0xFF98A2B3.toInt())   // 만기 초과
        dDayValue == 0L -> Pair("D-Day", 0xFFFF5A5F.toInt())
        dDayValue <= 7 -> Pair("D-$dDayValue", 0xFFFF5A5F.toInt())     // 7일 이내 긴박
        dDayValue <= 30 -> Pair("D-$dDayValue", 0xFFFF9A5C.toInt())    // 30일 이내 주의
        else -> Pair("D-$dDayValue", 0xFF98A2B3.toInt())               // 여유
    }

    // 상환 방식 한국어 변환
    val interestLabel = when (this.repaymentMethod) {
        "BULLET" -> "만기일시상환"
        "INSTALLMENT" -> "분할상환"
        "CUSTOM" -> "맞춤상환"
        else -> "-"
    }

    return ContractCardModel(
        id = this.contractSeq,
        status = status,
        role = role,
        counterparty = this.counterpartyName ?: "상대방",
        amount = amountText,
        dueDate = dueDateText,
        dDayLabel = dDayLabel,
        dDayColorHex = dDayColorHex,
        interestLabel = interestLabel
    )
}

package com.billage.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.ContractApi
import com.billage.app.data.remote.api.ContractListResponse
import com.billage.app.data.repository.NotificationRepository
import com.billage.app.data.repository.RepaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val userName: String = "빌리지",
    val lentCount: Int = 0,
    val borrowedCount: Int = 0,
    val upcomingSchedules: List<ScheduleItem> = emptyList(),
    val recentActivities: List<ActivityItem> = emptyList(),
    val hasNotification: Boolean = false,
    val notificationMessage: String = ""
)

data class ScheduleItem(
    val date: String,
    val name: String,
    val type: String,
    val amount: String
)

data class ActivityItem(
    val message: String,
    val timeAgo: String,
    val type: String
)

private data class UpcomingContractContext(
    val counterpartyName: String,
    val scheduleType: String
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contractApi: ContractApi,
    private val repaymentRepository: RepaymentRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshHome()
        observeUnreadCount()
        refreshUnreadCount()
    }

    fun refreshHome() {
        viewModelScope.launch {
            val contracts = runCatching {
                contractApi.getContracts(role = "ALL", status = null).data
            }.getOrElse { throwable ->
                throwable.printStackTrace()
                emptyList()
            }

            val confirmedStatuses = setOf("SIGNED", "ACTIVE", "OVERDUE", "DISPUTED")
            val lent = contracts.count { it.role == "CREDITOR" && it.status in confirmedStatuses }
            val borrowed = contracts.count { it.role == "DEBTOR" && it.status in confirmedStatuses }

            val contractContexts = contracts.associate { contract ->
                contract.contractSeq to contract.toUpcomingContractContext()
            }

            val mappedSchedules = runCatching {
                repaymentRepository.getUpcomingSchedules().map { schedule ->
                    val context = contractContexts[schedule.contractSeq]
                    ScheduleItem(
                        date = formatScheduleDate(schedule.dueDt),
                        name = context?.counterpartyName ?: "상대방",
                        type = context?.scheduleType ?: "드릴",
                        amount = formatScheduleAmount(schedule.totalDue)
                    )
                }
            }.getOrElse { throwable ->
                throwable.printStackTrace()
                emptyList()
            }

            _uiState.update {
                it.copy(
                    lentCount = lent,
                    borrowedCount = borrowed,
                    upcomingSchedules = mappedSchedules
                )
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            notificationRepository.unreadCount.collectLatest { unreadCount ->
                _uiState.update {
                    it.copy(
                        hasNotification = unreadCount > 0,
                        notificationMessage = if (unreadCount > 0) "새로운 알림이 있어요" else ""
                    )
                }
            }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            runCatching { notificationRepository.refreshUnreadCount() }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(hasNotification = false) }
    }
}

private fun ContractListResponse.toUpcomingContractContext(): UpcomingContractContext {
    return UpcomingContractContext(
        counterpartyName = counterpartyName?.takeIf { it.isNotBlank() } ?: "상대방",
        scheduleType = if (role == "CREDITOR") "받을" else "드릴"
    )
}

private fun formatScheduleDate(raw: String): String {
    return runCatching {
        LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("yyyy.M.d"))
    }.getOrElse {
        raw.replace("-", ".")
    }
}

private fun formatScheduleAmount(amount: Double): String {
    return "${"%,.0f".format(amount)}원"
}

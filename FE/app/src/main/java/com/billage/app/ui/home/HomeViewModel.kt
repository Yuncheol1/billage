package com.billage.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.ContractApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 홈 화면에 표시할 데이터 묶음
// 현재는 더미 데이터, 추후 API 연동 시 실제 데이터로 교체 예정
data class HomeUiState(
    val userName: String = "",                          // 사용자 이름
    val lentCount: Int = 0,                             // 받을 계약 건수 (채권자)
    val borrowedCount: Int = 0,                         // 갚을 계약 건수 (채무자)
    val upcomingSchedules: List<ScheduleItem> = emptyList(), // 다가오는 일정
    val recentActivities: List<ActivityItem> = emptyList(),  // 최근 거래활동
    val hasNotification: Boolean = false,               // 중요 알림 여부
    val notificationMessage: String = ""                // 중요 알림 내용
)

// 다가오는 일정 항목
// type: "받을" 또는 "드릴"
data class ScheduleItem(
    val date: String,       // 예) "2026.6.23"
    val name: String,       // 예) "김철수"
    val type: String,       // 예) "받을", "드릴"
    val amount: String      // 예) "30만원"
)

// 최근 거래활동 항목
// type: "transfer"(돈 받음) 또는 "chat"(협의 중)
data class ActivityItem(
    val message: String,    // 예) "김철수님이 돈을 보냈어요"
    val timeAgo: String,    // 예) "10분 전"
    val type: String        // "transfer" | "chat"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contractApi: ContractApi
) : ViewModel() {
    // StateFlow — 데이터가 바뀌면 Compose UI가 자동으로 다시 그려지게 해주는 객체
    // _uiState는 내부에서만 수정 가능 (private)
    private val _uiState = MutableStateFlow(HomeUiState())
    // uiState는 외부(HomeScreen)에서 읽기만 가능 (public)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDummyData()
        fetchContracts()
    }

    fun fetchContracts() {
        viewModelScope.launch {
            try {
                // status = null → CLOSED(상환 완료)를 포함한 전체 상태 조회
                // CREDITOR/DEBTOR 건수는 UI에서 role 필드로 구분
                val response = contractApi.getContracts(role = "ALL", status = null)
                if (response.status == 200) {
                    val contracts = response.data
                    // DRAFT·NEGOTIATING(서명 전 협의 단계)은 아직 확정된 계약이 아니므로 제외
                    val confirmedStatuses = setOf("SIGNED", "ACTIVE", "OVERDUE", "DISPUTED")
                    val lent = contracts.count { it.role == "CREDITOR" && it.status in confirmedStatuses }
                    val borrowed = contracts.count { it.role == "DEBTOR" && it.status in confirmedStatuses }
                    
                    _uiState.update { it.copy(
                        lentCount = lent,
                        borrowedCount = borrowed
                    ) }
                }
            } catch (e: Exception) {
                // 에러 발생 시 처리 (현재는 로그나 토스트 등 생략)
                e.printStackTrace()
            }
        }
    }

    private fun loadDummyData() {
        _uiState.update {
            it.copy(
                userName = "빌리지",
                hasNotification = true,
            notificationMessage = "김철수님의 상환 예정일이 내일입니다",
            upcomingSchedules = listOf(
                ScheduleItem("2026.6.23", "김철수", "받을", "30만원"),
                ScheduleItem("2026.6.24", "이영희", "드릴", "500만원"),
                ScheduleItem("2026.6.25", "박민수", "받을", "10만원")
            ),
            recentActivities = listOf(
                ActivityItem("김철수님이 돈을 보냈어요", "10분 전", "transfer"),
                ActivityItem("이영희님과 조율 중이에요", "1시간 전", "chat")
            )
            )
        }
    }

    // 알림 배너 닫기
    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(hasNotification = false)
    }
}
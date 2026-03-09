package com.billage.app.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 홈 화면에 표시할 데이터 묶음
// 현재는 더미 데이터, 추후 API 연동 시 실제 데이터로 교체 예정
data class HomeUiState(
    val userName: String = "",                          // 사용자 이름
    val lentCount: Int = 0,                             // 빌려준 건수
    val lentAmount: String = "",                        // 빌려준 총액
    val borrowedCount: Int = 0,                         // 빌려온 건수
    val borrowedAmount: String = "",                    // 빌려온 총액
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

class HomeViewModel : ViewModel() {
    // StateFlow — 데이터가 바뀌면 Compose UI가 자동으로 다시 그려지게 해주는 객체
    // _uiState는 내부에서만 수정 가능 (private)
    private val _uiState = MutableStateFlow(HomeUiState())
    // uiState는 외부(HomeScreen)에서 읽기만 가능 (public)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // ViewModel이 처음 생성될 때 더미 데이터를 넣어요.
        // TODO: API 연동 후 실제 데이터로 교체
        loadDummyData()
    }

    private fun loadDummyData() {
        _uiState.value = HomeUiState(
            userName = "빌리지",
            lentCount = 1,
            lentAmount = "300만원",
            borrowedCount = 1,
            borrowedAmount = "5000만원",
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

    // 알림 배너 닫기
    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(hasNotification = false)
    }
}
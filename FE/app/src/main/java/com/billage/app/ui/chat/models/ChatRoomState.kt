package com.billage.app.ui.chat.models

/**
 * 채팅방의 현재 대화 모드를 정의합니다.
 */
enum class ChatRoomMode {
    NORMAL,     // 일반 대화 모드
    CONTRACT    // 타겟 차용증 협의 모드
}

/**
 * 바텀 시트에서 선택할 수 있는 차용증 목록 요소입니다.
 */
data class ContractSummary(
    val contractId: String,
    val title: String,
    val targetUserName: String // 대상자 이름 (예: "김철수")
)

/**
 * 전체 채팅방의 상태를 관리하는 UI State 객체입니다.
 */
data class ChatRoomState(
    val roomId: String = "",
    val mode: ChatRoomMode = ChatRoomMode.NORMAL,
    val selectedContract: ContractSummary? = null
)

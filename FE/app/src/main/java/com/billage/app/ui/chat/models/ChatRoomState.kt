package com.billage.app.ui.chat.models

import com.billage.app.data.remote.api.ContractDetailResponse

/**
 * 채팅방의 현재 대화 모드를 정의합니다.
 */
enum class ChatRoomMode {
    NORMAL,     // 일반 대화 모드
    CONTRACT    // 타겟 차용증 협의 모드
}

data class ContractSummary(
    val contractSeq: Int,
    val contractId: String,
    val counterpartyName: String
)

/**
 * 전체 채팅방의 상태를 관리하는 UI State 객체입니다.
 */
data class ChatRoomState(
    val roomId: String = "",
    val roomName: String = "대화방",
    val mode: ChatRoomMode = ChatRoomMode.NORMAL,
    val selectedContract: ContractSummary? = null,
    val draftProposal: String? = null,
    val draftContractDetail: ContractDetailResponse? = null,
    val isMeAccepted: Boolean = false,
    val isOpponentAccepted: Boolean = false
)

package com.billage.app.ui.chat.models

import java.time.LocalDateTime

/**
 * 발신자 구분 (내 메시지, 상대방 메시지, 시스템 메시지)
 */
enum class MessageSender {
    ME, OTHER, SYSTEM
}

/**
 * 액션 버튼의 상태 (활성화/비활성화, 텍스트 변경 등)
 */
enum class ActionStatus {
    PENDING, // 대기 (클릭 가능)
    COMPLETED, // 완료 (비활성화 및 텍스트 변경)
    REJECTED // 거절됨 (비활성화)
}

/**
 * 채팅방 모든 카드의 공통 속성
 */
interface BaseChatMessage {
    val id: String
    val sender: MessageSender
    val timestamp: LocalDateTime
}

// ==========================================
// 도메인 특화: 미디어 메시지 모델
// ==========================================
data class FileMessage(
    override val id: String,
    override val sender: MessageSender,
    override val timestamp: LocalDateTime,
    val fileName: String,
    val fileSizeKb: Int,
    val fileExtension: String,
    val fileUrl: String
) : BaseChatMessage

data class ImageMessage(
    override val id: String,
    override val sender: MessageSender,
    override val timestamp: LocalDateTime,
    val imageUrl: String,
    val thumbnailWidth: Int,
    val thumbnailHeight: Int
) : BaseChatMessage

data class LinkMessage(
    override val id: String,
    override val sender: MessageSender,
    override val timestamp: LocalDateTime,
    val url: String,
    val ogTitle: String,
    val ogDescription: String,
    val ogImageUrl: String? = null
) : BaseChatMessage

// ==========================================
// 도메인 특화: 계약 핑퐁(인터랙티브) 메시지 모델
// ==========================================
data class DraftRequestMessage(
    override val id: String,
    override val sender: MessageSender,
    override val timestamp: LocalDateTime,
    val contractTitle: String,
    val actionStatus: ActionStatus = ActionStatus.PENDING
) : BaseChatMessage

data class ConditionProposalMessage(
    override val id: String,
    override val sender: MessageSender,
    override val timestamp: LocalDateTime,
    val modifications: List<String>, // 변경된 조항 리스트
    val actionStatus: ActionStatus = ActionStatus.PENDING
) : BaseChatMessage

data class SignatureRequestMessage(
    override val id: String,
    override val sender: MessageSender,
    override val timestamp: LocalDateTime,
    val isMySignatureDone: Boolean = false,
    val isOtherSignatureDone: Boolean = false
) : BaseChatMessage

data class ContractCompleteMessage(
    override val id: String,
    override val sender: MessageSender,
    override val timestamp: LocalDateTime,
    val contractId: String,
    val contractTitle: String
) : BaseChatMessage

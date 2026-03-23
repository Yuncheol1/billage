package com.billage.app.ui.chat.models

import kotlinx.serialization.Serializable

/**
 * AI_PROPOSAL 메시지의 content 필드에 JSON 직렬화되어 들어가는 페이로드
 * DB에 저장되고 양측에 실시간 브로드캐스트된다.
 */
@Serializable
data class AiProposalPayload(
    val contractTitle: String,
    val contractSeq: Int,
    val originalInterestRate: Double,
    val proposedInterestRate: Double,
    val modifications: List<String> = emptyList()
)

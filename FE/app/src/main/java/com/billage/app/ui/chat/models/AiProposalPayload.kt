package com.billage.app.ui.chat.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * AI_PROPOSAL 메시지의 content 필드에 JSON 직렬화되어 들어가는 페이로드
 * DB에 저장되고 양측에 실시간 브로드캐스트된다.
 */
@Serializable
data class AiProposalPayload(
    @SerialName("contract_title")
    @JsonNames("contractTitle")
    val contractTitle: String,
    
    @SerialName("contract_seq")
    @JsonNames("contractSeq")
    val contractSeq: Int,
    
    @SerialName("original_principal")
    @JsonNames("originalPrincipal")
    val originalPrincipal: Double? = null,
    
    @SerialName("proposed_principal")
    @JsonNames("proposedPrincipal")
    val proposedPrincipal: Double? = null,
    
    @SerialName("original_interest_rate")
    @JsonNames("originalInterestRate")
    val originalInterestRate: Double? = null,
    
    @SerialName("proposed_interest_rate")
    @JsonNames("proposedInterestRate")
    val proposedInterestRate: Double? = null,
    
    val modifications: List<String> = emptyList(),
    
    @SerialName("is_initial")
    @JsonNames("isInitial")
    val isInitial: Boolean = false
)

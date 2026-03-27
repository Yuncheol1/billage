package com.billage.app.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.ChatApi
import com.billage.app.data.remote.api.ContractApi
import com.billage.app.data.remote.api.ContractDetailResponse
import com.billage.app.data.remote.api.ContractSignatureRequest
import com.billage.app.data.remote.api.ContractListResponse
import com.billage.app.data.remote.api.ContractUpdateRequest
import com.billage.app.data.remote.api.ClauseUpdateDto
import com.billage.app.data.remote.api.AiClauseResponse
import com.billage.app.data.repository.ChatRepository
import com.billage.app.data.repository.ContractRepository
import com.billage.app.ui.chat.models.AiProposalMessage
import com.billage.app.ui.chat.models.AiProposalPayload
import com.billage.app.ui.chat.models.BaseChatMessage
import com.billage.app.ui.chat.models.ChatMessageRes
import com.billage.app.ui.chat.models.ChatRoomMode
import com.billage.app.ui.chat.models.ChatRoomState
import com.billage.app.ui.chat.models.ContractSummary
import com.billage.app.ui.chat.models.MessageSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import com.billage.app.data.local.AuthSessionStorage
import retrofit2.HttpException
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val chatApi: ChatApi,
    private val contractApi: ContractApi,
    private val userApi: com.billage.app.data.remote.api.UserApi,
    private val chatRepository: ChatRepository,
    private val contractRepository: ContractRepository,
    private val authSessionStorage: AuthSessionStorage
) : ViewModel() {

    companion object {
        private const val TAG = "ChatRoomViewModel"
    }

    val myUserSeq: Int get() = authSessionStorage.getUserSeq()

    private val _messages = MutableStateFlow<List<ChatMessageRes>>(emptyList())
    val messages: StateFlow<List<ChatMessageRes>> = _messages.asStateFlow()

    private val _contracts = MutableStateFlow<List<ContractListResponse>>(emptyList())
    val contracts: StateFlow<List<ContractListResponse>> = _contracts.asStateFlow()

    private val _chatRoomState = MutableStateFlow(ChatRoomState())
    val chatRoomState: StateFlow<ChatRoomState> = _chatRoomState.asStateFlow()

    // 로컴 (UI 전용) 카드리스트 — AI 제안서등 를실시간 채팅스트림에 눁는다
    private val _localCards = MutableStateFlow<List<BaseChatMessage>>(emptyList())
    val localCards: StateFlow<List<BaseChatMessage>> = _localCards.asStateFlow()

    private var isInitialProposalSent = false

    fun joinRoom(roomId: String, roomName: String, isNewContract: Boolean = false, contractSeq: Int? = null) {
        if (isNewContract) isInitialProposalSent = false
        if (_chatRoomState.value.roomId == roomId && _messages.value.isNotEmpty() && !isNewContract) return 
        _chatRoomState.update { it.copy(roomId = roomId, roomName = roomName) }
        Log.d(TAG, "Joining room: $roomId (isNewContract: $isNewContract, contractSeq: $contractSeq)")

        viewModelScope.launch {
            // 0. 읽음 처리
            try {
                chatApi.markAsRead(myUserSeq, roomId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark as read", e)
            }

            // 1. 과거 메시지 내역 로드
            try {
                val history = chatApi.getChatHistory(roomId)
                _messages.value = history
                
                // 만약 히스토리가 비어있고 신규 계약이라면 잠시 후 다시 확인 (백엔드 처리 시간 고려)
                if (isNewContract && history.isEmpty()) {
                    kotlinx.coroutines.delay(500)
                    val retryHistory = chatApi.getChatHistory(roomId)
                    if (retryHistory.isNotEmpty()) {
                        _messages.value = retryHistory
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load history", e)
            }

            // 2. 만약 새 계약이라면, 최초 카드 제안 전송
            // -1 과 같은 유효하지 않은 ID 무시
            val validContractSeq = if (contractSeq != null && contractSeq > 0) contractSeq else null

            // 상태 동기화는 카드 발송 여부와 상관없이 항상 시도
            try {
                val syncTargetSeq = validContractSeq ?: run {
                    val res = contractApi.getContracts()
                    if (res.status == 200 && res.data.isNotEmpty()) res.data.first().contractSeq else null
                }
                if (syncTargetSeq != null && syncTargetSeq > 0) {
                    val detailRes = contractApi.getContractDetail(syncTargetSeq)
                    if (detailRes.status == 200) {
                        applyContractDetail(detailRes.data)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync initial contract state", e)
            }

            // 제안 카드 발송 로직 (isNewContract && 아직 이 계약에 대한 제안이 없는 경우)
            if (isNewContract && validContractSeq != null) {
                try {
                    // 동일한 contractSeq에 대해 이미 제안이 발송되었는지 히스토리에서 확인
                    val alreadyProposed = _messages.value.any { 
                        it.msgType == "AI_PROPOSAL" && it.content.contains("\"contract_seq\":$validContractSeq") 
                    }
                    
                    if (!alreadyProposed) {
                        val detailRes = contractApi.getContractDetail(validContractSeq)
                        if (detailRes.status == 200) {
                            val detail = detailRes.data

                            // STOMP 세션이 준비될 때까지 대기 (메시지 유실 방지)
                            val session = chatRepository.session.filterNotNull().first()
                            
                            // 실명 로드
                            var senderName = authSessionStorage.getUserName()
                            if (senderName == null) {
                                try {
                                    val userRes = userApi.getMyInfo()
                                    if (userRes.status == 200) {
                                        senderName = userRes.data.realName
                                        senderName?.let { authSessionStorage.saveUserName(it) }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to fetch user name", e)
                                }
                            }

                            val originalPrincipal = detail.principalAmount ?: 0.0
                            val originalInterest = detail.interestRate ?: 0.0
                            
                            val senderRole = detail.myRole ?: "CREDITOR"

                            val payload = AiProposalPayload(
                                contractTitle = "빌리지 거래 제안서",
                                contractSeq = detail.contractSeq,
                                originalPrincipal = originalPrincipal,
                                proposedPrincipal = originalPrincipal,
                                originalInterestRate = originalInterest,
                                proposedInterestRate = originalInterest,
                                senderRole = senderRole,
                                senderName = senderName ?: "나",
                                modifications = emptyList(),
                                isInitial = true
                            )
                            val contentJson = Json.encodeToString(AiProposalPayload.serializer(), payload)
                            
                            // 최종 발송 시점에서도 한 번 더 체크 (isInitialProposalSent는 세션 내 중복 방지용)
                            if (isInitialProposalSent) {
                                return@launch
                            }
                            isInitialProposalSent = true

                            chatRepository.sendMessage(
                                roomId = roomId,
                                senderSeq = myUserSeq,
                                content = contentJson,
                                isContractMsg = true,
                                msgType = "AI_PROPOSAL"
                            )
                            
                            // 로컬 목록에 즉시 추가하는 대신 STOMP 리턴을 기다림 (중복 표시 방지)
                            // AI 제안서는 시스템에 의해 자동 발송되므로 0.5~1초 뒤에 보여도 무방함
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send initial contract card", e)
                }
            }

            // 3. STOMP 세션 관찰 및 구독
            chatRepository.session.collect { session ->
                session?.let {
                    subscribeToRoom(it, roomId)
                }
            }
        }
    }

    private fun subscribeToRoom(session: StompSession, roomId: String) {
        viewModelScope.launch {
            try {
                val subscription = session.subscribe(StompSubscribeHeaders(destination = "/sub/chat/room/$roomId"))
                Log.d(TAG, "Subscribed successfully to $roomId")

                subscription.collect { frame ->
                    val payload = frame.bodyAsText
                    Log.d(TAG, "Received frame: $payload")
                    try {
                        val json = Json { ignoreUnknownKeys = true }
                        val receivedMsg = json.decodeFromString<ChatMessageRes>(payload)

                        // 수정 제안 메시지가 오면 수락 상태 초기화
                        if (receivedMsg.msgType == "AI_PROPOSAL") {
                             _chatRoomState.update {
                                 it.copy(
                                     isMeAccepted = false,
                                     isOpponentAccepted = false,
                                     isMeSigned = false,
                                     isOpponentSigned = false
                                 )
                             }
                        }

                        // 서버에서 보낸 시스템 메시지(수락 알림 등) 처리
                        if (receivedMsg.msgType == "SYSTEM" && shouldRefreshContractState(receivedMsg.content)) {
                            // 모든 메시지 수신 시 상태 최신화 시도
                            _chatRoomState.value.draftContractDetail?.contractSeq?.let { seq ->
                                fetchContractDetail(seq)
                            } ?: run {
                                // 만약 현재 열린 상세가 없다면 첫번째 계약이라도 동기화 시도 (안전 장치)
                                fetchContracts()
                            }
                        }

                        _messages.update { currentList ->
                            if (currentList.any { it.id == receivedMsg.id }) currentList
                            else currentList + receivedMsg
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Decode error", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Subscription error", e)
                chatRepository.clearSession()
                chatRepository.connect()
            }
        }
    }

    fun sendMessage(content: String) {
        val currentState = _chatRoomState.value
        val currentRoomId = currentState.roomId
        if (currentRoomId.isEmpty() || content.isBlank()) return
        
        // 협의 모드일 때는 isContractMsg = true 로 보내어 나중에 AI가 분석할 수 있게 함
        val isContractMsg = currentState.mode == ChatRoomMode.CONTRACT
        chatRepository.sendMessage(
            roomId = currentRoomId, 
            senderSeq = myUserSeq, 
            content = content,
            isContractMsg = isContractMsg
        )
    }

    fun setModificationMode(contract: ContractListResponse) {
        _chatRoomState.update { it.copy(
            mode = ChatRoomMode.CONTRACT,
            selectedContract = ContractSummary(
                contractSeq = contract.contractSeq,
                contractId = contract.contractId ?: "ID없음",
                counterpartyName = contract.counterpartyName ?: "미상"
            ),
            isMeAccepted = false,
            isOpponentAccepted = false,
            isMeSigned = false,
            isOpponentSigned = false
        )}
    }

    fun setMode(mode: ChatRoomMode) {
        _chatRoomState.update { it.copy(mode = mode) }
    }


    fun requestAiProposal() {
        val contract = _chatRoomState.value.selectedContract ?: return
        val roomId = _chatRoomState.value.roomId
        viewModelScope.launch {
            try {
                // 1. 백엔드에 Mock AI 제안 생성 요청
                val res = contractRepository.createAiProposalMock(contract.contractSeq)
                if (res.status == 200) {
                    // 2. 계약 상세 재조회하여 aiDraftContent 갱신
                    val updatedContract = contractRepository.getContractDetail(contract.contractSeq)

                    if (updatedContract.aiDraftContent != null) {
                        // 3. State 업데이트
                        _chatRoomState.update { it.copy(
                            draftProposal = updatedContract.aiDraftContent,
                            draftContractDetail = updatedContract,
                            mode = ChatRoomMode.NORMAL,
                            isMeAccepted = false,
                            isOpponentAccepted = false,
                            isMeSigned = false,
                            isOpponentSigned = false
                        )}

                        // 4. AI 제안 내용을 JSON 으로 직렬화해 STOMP 로 전송 (→ DB 저장 → 양측 수신)
                        val originalPrincipal = updatedContract.principalAmount ?: 0.0
                        val originalInterest = updatedContract.interestRate ?: 0.0
                        
                        val req = try {
                            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                                .decodeFromString<com.billage.app.data.remote.api.ContractUpdateRequest>(updatedContract.aiDraftContent)
                        } catch (e: Exception) { null }

                        val (proposedPrincipal, proposedInterest) = if (req != null) {
                            (req.principalAmount ?: originalPrincipal) to (req.interestRate ?: originalInterest)
                        } else {
                            originalPrincipal to originalInterest
                        }

                        val modifications = mutableListOf<String>()
                        if (originalPrincipal != proposedPrincipal) {
                            modifications.add(String.format("차용 원금 조정 제안: %,.0f원 → %,.0f원", originalPrincipal, proposedPrincipal))
                        }
                        if (Math.abs(originalInterest - proposedInterest) > 0.0001) {
                            modifications.add(String.format("이자율 조정 제안: %.2f%% → %.2f%%", originalInterest * 100, proposedInterest * 100))
                        }
                        
                        // 특약 사항도 modifications 에 번호(조) 붙여서 추가
                        req?.clauses?.forEachIndexed { index, clause ->
                            modifications.add("제${index + 1}조 (특약) ${clause.content.take(20)}${if (clause.content.length > 20) "..." else ""}")
                        }

                        if (modifications.isEmpty()) {
                            modifications.add("계약 조건 검토 제안")
                        }

                        val detail = _chatRoomState.value.draftContractDetail
                        val senderRole = detail?.myRole ?: "CREDITOR"
                        
                        var senderName = authSessionStorage.getUserName()
                        if (senderName == null) {
                            try {
                                val userRes = userApi.getMyInfo()
                                if (userRes.status == 200) {
                                    senderName = userRes.data.realName
                                    senderName?.let { authSessionStorage.saveUserName(it) }
                                }
                            } catch (e: Exception) {}
                        }

                        // content = JSON 페이로드, msgType = AI_PROPOSAL
                        val payload = AiProposalPayload(
                            contractTitle = "빌리지 거래 제안서",
                            contractSeq = contract.contractSeq,
                            originalPrincipal = originalPrincipal,
                            proposedPrincipal = proposedPrincipal,
                            originalInterestRate = originalInterest,
                            proposedInterestRate = proposedInterest,
                            senderRole = senderRole,
                            senderName = senderName ?: "나",
                            modifications = modifications,
                            isInitial = false
                        )
                        val contentJson = kotlinx.serialization.json.Json.encodeToString(
                            AiProposalPayload.serializer(), payload
                        )
                        chatRepository.sendMessage(
                            roomId = roomId,
                            senderSeq = myUserSeq,
                            content = contentJson,
                            isContractMsg = true,
                            msgType = "AI_PROPOSAL"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request AI proposal", e)
            }
        }
    }

    /**
     * AI 협의 모드로 진입합니다. (협의할 차용증 선택 직후 호출)
     */
    fun startAiNegotiation(summary: ContractSummary) {
        val roomId = _chatRoomState.value.roomId
        val userSeq = myUserSeq
        
        _chatRoomState.update { 
            it.copy(
                mode = ChatRoomMode.CONTRACT,
                selectedContract = summary,
                pendingAiClauses = null,
                selectedClauseIndices = emptySet(),
                isMeAccepted = false,
                isOpponentAccepted = false,
                isMeSigned = false,
                isOpponentSigned = false
            ) 
        }

        // 타임라인 필터링을 위한 마커 메시지 발송
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(
                    roomId = roomId,
                    senderSeq = userSeq,
                    content = "AI 협의를 시작합니다.",
                    isContractMsg = true,
                    msgType = "SYSTEM"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send negotiation start marker", e)
            }
        }
    }

    /**
     * 지금까지의 대화(isContractMsg=true)를 바탕으로 AI 특약 분석을 요청합니다.
     */
    fun analyzeCurrentNegotiation() {
        val currentState = _chatRoomState.value
        val roomId = currentState.roomId
        val contract = currentState.selectedContract ?: return
        
        viewModelScope.launch {
            _chatRoomState.update { it.copy(isAiAnalyzing = true, aiRejectionReason = null) }
            try {
                val res = contractApi.generateClausesFromChat(contract.contractSeq, roomId)
                if (res.status == 200 && res.data != null) {
                    val aiData = res.data
                    if (aiData.rejected) {
                        _chatRoomState.update { 
                            it.copy(
                                pendingAiClauses = emptyList(),
                                aiRejectionReason = aiData.rejection_reason ?: "합의 내용을 추출할 수 없습니다. 대화를 더 진행해 보세요."
                            )
                        }
                    } else {
                        val clauses = aiData.clauses ?: emptyList()
                        _chatRoomState.update { 
                            it.copy(
                                pendingAiClauses = clauses,
                                selectedClauseIndices = clauses.indices.toSet(),
                                aiRejectionReason = null
                            ) 
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to analyze negotiation", e)
            } finally {
                _chatRoomState.update { it.copy(isAiAnalyzing = false) }
            }
        }
    }

    /**
     * AI가 제안된 특약 중 특정 항목의 선택 여부를 토글합니다.
     */
    fun toggleClauseSelection(index: Int) {
        _chatRoomState.update { state ->
            val current = state.selectedClauseIndices.toMutableSet()
            if (current.contains(index)) current.remove(index) else current.add(index)
            state.copy(selectedClauseIndices = current)
        }
    }

    /**
     * 선택된 AI 특약들을 확정하여 DB에 즉시 반영하고 채팅방에 공유합니다.
     */
    fun applySelectedAiClauses() {
        val state = _chatRoomState.value
        val contract = state.selectedContract ?: return
        val pendingAiClauses = state.pendingAiClauses ?: return
        val selectedIdx = state.selectedClauseIndices
        val roomId = state.roomId

        if (selectedIdx.isEmpty()) {
            exitModificationMode()
            return
        }

        viewModelScope.launch {
            try {
                // 1. 현재 계약 상세 정보 가져오기
                val currentDetail = state.draftContractDetail ?: return@launch
                
                // 2. 기본값 설정 (현재 값 유지)
                var proposedPrincipal = currentDetail.principalAmount
                var proposedInterest = currentDetail.interestRate
                
                // 3. 기존 특약들을 Update DTO로 변환하여 유지
                val finalClauses = mutableListOf<ClauseUpdateDto>()
                currentDetail.clauses.forEach { clause ->
                    finalClauses.add(ClauseUpdateDto(clauseSeq = clause.clauseSeq, content = clause.content))
                }
                
                // 4. 선택된 AI 제안 특약 처리
                pendingAiClauses.filterIndexed { index, _ -> selectedIdx.contains(index) }
                    .forEach { aiClause ->
                        val content = aiClause.legalClause ?: ""
                        val clauseId = aiClause.clauseId
                        
                        var handledByField = false
                        
                        // Heuristic: 이자율 관련 조항 텍스트에서 숫자 추출 (마지막 매칭 항목을 선택하여 '변경 후' 값을 잡음)
                        if (clauseId == "INTEREST") {
                            // %, 퍼센트, 퍼 등 지원
                            val interestRegex = """(\d+(\.\d+)?)\s*(?:%|퍼센트|퍼)""".toRegex()
                            val matches = interestRegex.findAll(content).toList()
                            if (matches.isNotEmpty()) {
                                val lastMatch = matches.last()
                                val value = lastMatch.groupValues[1].toDoubleOrNull()
                                if (value != null) {
                                    proposedInterest = value / 100.0
                                    handledByField = true
                                    Log.d(TAG, "Extracted LAST interest rate: $proposedInterest from $content")
                                }
                            }
                        }
                        
                        // Heuristic: 원금 관련 조항 텍스트에서 숫자 추출 (마지막 매칭 항목 선택)
                        if (clauseId == "PRINCIPAL") {
                            // "1000000원" 또는 "100만원" 형태 지원
                            val manWonRegex = """(\d+(\.\d+)?)\s*만원""".toRegex()
                            val wonRegex = """(\d+([,]\d+)*)\s*원""".toRegex()
                            
                            val manMatches = manWonRegex.findAll(content).toList()
                            val wonMatches = wonRegex.findAll(content).toList()
                            
                            if (manMatches.isNotEmpty()) {
                                val lastMatch = manMatches.last()
                                val value = lastMatch.groupValues[1].toDoubleOrNull()
                                if (value != null) {
                                    proposedPrincipal = value * 10000.0
                                    handledByField = true
                                    Log.d(TAG, "Extracted LAST principal (man-won): $proposedPrincipal from $content")
                                }
                            } else if (wonMatches.isNotEmpty()) {
                                val lastMatch = wonMatches.last()
                                val value = lastMatch.groupValues[1].replace(",", "").toDoubleOrNull()
                                if (value != null) {
                                    proposedPrincipal = value
                                    handledByField = true
                                    Log.d(TAG, "Extracted LAST principal (won): $proposedPrincipal from $content")
                                }
                            }
                        }
                        
                        // 필드 업데이트로 처리되지 않은 경우에만 별도 특약으로 추가
                        if (!handledByField) {
                            finalClauses.add(ClauseUpdateDto(clauseSeq = 0, content = content))
                        }
                    }

                val updateRequest = ContractUpdateRequest(
                    principalAmount = proposedPrincipal,
                    interestRate = proposedInterest,
                    clauses = finalClauses
                )

                // updateContract 호출
                val response = contractApi.updateContract(contract.contractSeq, updateRequest, roomId)
                
                if (response.status == 200) {
                    // 2. 최신 계약 정보 다시 불러오기 (수락 상태 등이 초기화된 최신 정보)
                    fetchContractDetail(contract.contractSeq)
                    
                    // 3. 협의 모드 종료
                    exitModificationMode()
                    Log.d(TAG, "AI clauses applied directly to contract ${contract.contractSeq}")
                } else {
                    Log.e(TAG, "Failed to apply AI clauses: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in applySelectedAiClauses", e)
            }
        }
    }

    fun exitModificationMode() {
        _chatRoomState.update { it.copy(
            mode = ChatRoomMode.NORMAL,
            selectedContract = null,
            draftProposal = null,
            pendingAiClauses = null,
            selectedClauseIndices = emptySet()
        )}
    }

    fun submitProposal() {
        val currentContract = _chatRoomState.value.draftContractDetail ?: return
        val roomId = _chatRoomState.value.roomId
        
        viewModelScope.launch {
            try {
                // 1. 서버에 수락 요청 (서버가 DB 업데이트 + SYSTEM 메시지 발송)
                val res = contractApi.acceptContract(currentContract.contractSeq, roomId)
                if (res.status == 200) {
                    // 2. 상태 동기화
                    applyContractDetail(res.data)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to accept contract", e)
            } finally {
                exitModificationMode()
            }
        }
    }

    fun submitContractSignature(request: ContractSignatureRequest) {
        val currentState = _chatRoomState.value
        val contractSeq = currentState.draftContractDetail?.contractSeq ?: currentState.selectedContract?.contractSeq

        if (contractSeq == null) {
            _chatRoomState.update {
                it.copy(signatureErrorMessage = "서명할 계약 정보를 찾을 수 없습니다.")
            }
            return
        }

        _chatRoomState.update {
            it.copy(
                isSubmittingSignature = true,
                signatureErrorMessage = null,
                signatureSuccessMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val response = contractApi.submitSignature(contractSeq, request)
                if (response.status == 201) {
                    runCatching { contractApi.getContractDetail(contractSeq) }
                        .onSuccess { detailResponse ->
                            if (detailResponse.status == 200) {
                                applyContractDetail(detailResponse.data)
                            }
                        }

                    val roomId = _chatRoomState.value.roomId
                    if (roomId.isNotBlank()) {
                        if (response.data.contractFullySigned) {
                            val detail = _chatRoomState.value.draftContractDetail
                            val payload = com.billage.app.ui.chat.models.ContractCompletePayload(
                                contractId = detail?.contractId ?: "ID없음",
                                contractTitle = "빌리지 차용 상환 계약",
                                principalAmount = detail?.principalAmount ?: 0.0,
                                interestRate = detail?.interestRate ?: 0.0,
                                contractSeq = contractSeq
                            )
                            val contentJson = kotlinx.serialization.json.Json.encodeToString(
                                com.billage.app.ui.chat.models.ContractCompletePayload.serializer(), payload
                            )
                            chatRepository.sendMessage(
                                roomId = roomId,
                                senderSeq = myUserSeq,
                                content = contentJson,
                                isContractMsg = true,
                                msgType = "CONTRACT_COMPLETE"
                            )
                        } else {
                            chatRepository.sendMessage(
                                roomId = roomId,
                                senderSeq = myUserSeq,
                                content = "전자서명을 완료했습니다.",
                                isContractMsg = true,
                                msgType = "SYSTEM"
                            )
                        }
                    }

                    _chatRoomState.update {
                        it.copy(
                            isSubmittingSignature = false,
                            isMeSigned = true,
                            signatureSuccessMessage = response.message
                        )
                    }
                } else {
                    _chatRoomState.update {
                        it.copy(
                            isSubmittingSignature = false,
                            signatureErrorMessage = response.message
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit signature", e)
                _chatRoomState.update {
                    it.copy(
                        isSubmittingSignature = false,
                        signatureErrorMessage = extractApiErrorMessage(e, "전자서명 저장에 실패했습니다.")
                    )
                }
            }
        }
    }

    fun clearSignatureFeedback() {
        _chatRoomState.update {
            it.copy(
                signatureErrorMessage = null,
                signatureSuccessMessage = null
            )
        }
    }
    
    fun cancelProposal() {
        _chatRoomState.update { it.copy(draftProposal = null) }
    }

    fun fetchContractDetail(contractSeq: Int) {
        viewModelScope.launch {
            try {
                val res = contractApi.getContractDetail(contractSeq)
                if (res.status == 200) {
                    applyContractDetail(res.data)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch contract detail", e)
            }
        }
    }

    fun fetchContracts() {
        viewModelScope.launch {
            try {
                val res = contractApi.getContracts()
                if (res.status == 200) {
                    _contracts.value = res.data
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load contracts", e)
            }
        }
    }

    private fun applyContractDetail(detail: ContractDetailResponse) {
        _chatRoomState.update {
            it.copy(
                draftContractDetail = detail,
                isMeAccepted = detail.isMeAccepted ?: false,
                isOpponentAccepted = detail.isOpponentAccepted ?: false,
                isMeSigned = detail.isMeSigned ?: false,
                isOpponentSigned = detail.isOpponentSigned ?: false
            )
        }
        // AI 분석 결과 가져오기
        fetchAiAnalysis(detail.contractSeq)
    }

    fun fetchAiAnalysis(contractSeq: Int) {
        viewModelScope.launch {
            _chatRoomState.update { it.copy(isAiAnalyzing = true, aiAnalysisResult = null) }
            try {
                val response = contractApi.getAiAnalysis(contractSeq)
                if (response.status == 200) {
                    val parsedResult = try {
                        kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                            .decodeFromString<com.billage.app.domain.model.AiAnalysisResult>(response.data)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse AI analysis JSON: ${response.data}", e)
                        null
                    }
                    
                    _chatRoomState.update {
                        it.copy(
                            aiAnalysisResult = parsedResult,
                            rawAiAnalysisJson = response.data
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch AI analysis", e)
            } finally {
                _chatRoomState.update { it.copy(isAiAnalyzing = false) }
            }
        }
    }

    private fun shouldRefreshContractState(content: String): Boolean {
        return content.contains("수락") || content.contains("서명") || content.contains("체결")
    }

    private fun extractApiErrorMessage(throwable: Throwable, fallback: String): String {
        return when (throwable) {
            is HttpException -> {
                val rawBody = throwable.response()?.errorBody()?.string()
                if (rawBody.isNullOrBlank()) {
                    fallback
                } else {
                    runCatching {
                        Json.parseToJsonElement(rawBody)
                            .jsonObject["message"]
                            ?.jsonPrimitive
                            ?.content
                    }.getOrNull() ?: fallback
                }
            }
            else -> throwable.message ?: fallback
        }
    }
}

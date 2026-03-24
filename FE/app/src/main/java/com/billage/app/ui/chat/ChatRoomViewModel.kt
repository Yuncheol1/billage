package com.billage.app.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.ChatApi
import com.billage.app.data.remote.api.ContractApi
import com.billage.app.data.remote.api.ContractListResponse
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
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import com.billage.app.data.local.AuthSessionStorage
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    private val chatApi: ChatApi,
    private val contractApi: ContractApi,
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

    fun joinRoom(roomId: String, roomName: String, isNewContract: Boolean = false, contractSeq: Int? = null) {
        if (_chatRoomState.value.roomId == roomId && _messages.value.isNotEmpty()) return 
        _chatRoomState.update { it.copy(roomId = roomId, roomName = roomName) }
        Log.d(TAG, "Joining room: $roomId (isNewContract: $isNewContract)")

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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load history", e)
            }

            // 2. 만약 새 계약이라면, 최초 카드 제안 전송
            if (isNewContract && _messages.value.none { it.msgType == "AI_PROPOSAL" }) {
                try {
                    val targetSeq = contractSeq ?: run {
                        // 만약 contractSeq 가 없으면 이전처럼 목록에서 첫번째 시도
                        val res = contractApi.getContracts()
                        if (res.status == 200 && res.data.isNotEmpty()) res.data.first().contractSeq else null
                    }

                    if (targetSeq != null) {
                        // 상세조회로 interestRate 확보
                        val detailRes = contractApi.getContractDetail(targetSeq)
                        if (detailRes.status == 200) {
                            val detail = detailRes.data
                            _chatRoomState.update { it.copy(
                                isMeAccepted = detail.isMeAccepted ?: false,
                                isOpponentAccepted = detail.isOpponentAccepted ?: false,
                                draftContractDetail = detail
                            ) }

                            val originalInterest = detail.interestRate ?: 0.0
                            val payload = AiProposalPayload(
                                contractTitle = "빌리 거래 제안서",
                                contractSeq = detail.contractSeq,
                                originalInterestRate = originalInterest,
                                proposedInterestRate = originalInterest,
                                modifications = listOf("최초 차용증 생성"),
                                isInitial = true
                            )
                            val contentJson = Json.encodeToString(AiProposalPayload.serializer(), payload)
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
                    Log.e(TAG, "Failed to send initial contract card", e)
                }
            } else {
                // 이미 메시지가 있는 경우에도 상태 동기화는 필요
                try {
                    val targetSeq = contractSeq ?: run {
                        val res = contractApi.getContracts()
                        if (res.status == 200 && res.data.isNotEmpty()) res.data.first().contractSeq else null
                    }
                    if (targetSeq != null) {
                        val detailRes = contractApi.getContractDetail(targetSeq)
                        if (detailRes.status == 200) {
                            val detail = detailRes.data
                            _chatRoomState.update { it.copy(
                                isMeAccepted = detail.isMeAccepted ?: false,
                                isOpponentAccepted = detail.isOpponentAccepted ?: false,
                                draftContractDetail = detail
                            ) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync acceptance state", e)
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
                             _chatRoomState.update { it.copy(isMeAccepted = false, isOpponentAccepted = false) }
                        }

                        // 서버에서 보낸 시스템 메시지(수락 알림 등) 처리
                        if (receivedMsg.msgType == "SYSTEM" && receivedMsg.content.contains("수락")) {
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
        val currentRoomId = _chatRoomState.value.roomId
        if (currentRoomId.isEmpty() || content.isBlank()) return
        chatRepository.sendMessage(currentRoomId, myUserSeq, content)
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
            isOpponentAccepted = false
        )}
    }

    fun exitModificationMode() {
        _chatRoomState.update { it.copy(
            mode = ChatRoomMode.NORMAL,
            selectedContract = null,
            draftProposal = null
        )}
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
                            isOpponentAccepted = false
                        )}

                        // 4. AI 제안 내용을 JSON 으로 직렬화해 STOMP 로 전송 (→ DB 저장 → 양측 수신)
                        val originalInterest = updatedContract.interestRate ?: 0.0
                        val proposedInterest = try {
                            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                                .decodeFromString<com.billage.app.data.remote.api.ContractUpdateRequest>(updatedContract.aiDraftContent)
                                .interestRate ?: originalInterest
                        } catch (e: Exception) { originalInterest }

                        // content = JSON 페이로드, msgType = AI_PROPOSAL
                        val payload = AiProposalPayload(
                            contractTitle = "빌리지 거래 제안서",
                            contractSeq = contract.contractSeq,
                            originalInterestRate = originalInterest,
                            proposedInterestRate = proposedInterest,
                            modifications = listOf("이자율 조정 제안")
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

    fun submitProposal() {
        val currentContract = _chatRoomState.value.draftContractDetail ?: return
        val roomId = _chatRoomState.value.roomId
        
        viewModelScope.launch {
            try {
                // 1. 서버에 수락 요청 (서버가 DB 업데이트 + SYSTEM 메시지 발송)
                val res = contractApi.acceptContract(currentContract.contractSeq, roomId)
                if (res.status == 200) {
                    // 2. 상태 동기화
                    val detail = res.data
                    _chatRoomState.update { it.copy(
                        isMeAccepted = detail.isMeAccepted ?: false,
                        isOpponentAccepted = detail.isOpponentAccepted ?: false,
                        draftContractDetail = detail
                    ) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to accept contract", e)
            } finally {
                exitModificationMode()
            }
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
                    val detail = res.data
                    _chatRoomState.update { it.copy(
                        draftContractDetail = detail,
                        isMeAccepted = detail.isMeAccepted ?: false,
                        isOpponentAccepted = detail.isOpponentAccepted ?: false
                    ) }
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
}

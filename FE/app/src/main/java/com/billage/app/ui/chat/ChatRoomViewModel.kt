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

    fun joinRoom(roomId: String, roomName: String) {
        if (_chatRoomState.value.roomId == roomId && _messages.value.isNotEmpty()) return 
        _chatRoomState.update { it.copy(roomId = roomId, roomName = roomName) }
        Log.d(TAG, "Joining room: $roomId")

        viewModelScope.launch {
            // 0. 읽음 처리
            try {
                chatApi.markAsRead(myUserSeq, roomId)
                Log.d(TAG, "Marked as read: $roomId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark as read", e)
            }

            // 1. 과거 메시지 내역 로드 (REST API)
            try {
                val history = chatApi.getChatHistory(roomId)
                _messages.value = history
                Log.d(TAG, "History loaded: ${history.size} messages")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load history", e)
            }

            // 2. STOMP 세션 관찰 및 구독
            chatRepository.session.collect { session ->
                session?.let {
                    Log.d(TAG, "Session ready, subscribing to /sub/chat/room/$roomId")
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
            )
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
                            mode = ChatRoomMode.NORMAL
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
                            contractTitle = "계약 #${updatedContract.contractId ?: contract.contractSeq}",
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
        val draft = _chatRoomState.value.draftProposal ?: return
        val roomId = _chatRoomState.value.roomId
        
        // 수락 알림을 SYSTEM 메시지로 전송 (가운데 정렬)
        chatRepository.sendMessage(
            roomId = roomId,
            senderSeq = myUserSeq,
            content = "계약 수정 제안이 수락되었습니다. 전자서명 단계로 이동합니다.",
            isContractMsg = true,
            msgType = "SYSTEM"
        )
        
        // 전송 후 완료 처리
        cancelProposal()
        exitModificationMode()
    }
    
    fun cancelProposal() {
        _chatRoomState.update { it.copy(draftProposal = null) }
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

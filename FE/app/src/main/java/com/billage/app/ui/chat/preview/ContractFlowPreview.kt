package com.billage.app.ui.chat.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.billage.app.ui.chat.ChatRoomScreen
import com.billage.app.ui.chat.components.ContractDocumentFullScreen
import com.billage.app.ui.chat.components.cards.BaseChatCard
import com.billage.app.ui.chat.components.cards.ConditionProposalCard
import com.billage.app.ui.chat.components.cards.DraftRequestCard
import com.billage.app.ui.chat.components.cards.ProposalRequestCard
import com.billage.app.ui.chat.components.cards.SystemNoticeCard
import com.billage.app.ui.chat.models.*
import com.billage.app.ui.theme.BillageTheme
import java.time.LocalDateTime

/**
 * 하이브리드 UX(챗봇 기반 조건 수정) 프로세스와 전체 채팅방 UI를 보여주는 통합 프리뷰
 */
@Preview(showBackground = true, showSystemUi = true, name = "AI Contract Flow with Full UI")
@Composable
fun ContractFlowPreview() {
    BillageTheme(dynamicColor = false) {
        
        // 프리뷰 데모용 상태 관리
        // 컴포즈 Static Preview에서도 상단 배너를 예쁘게 볼 수 있도록 기본값을 CONTRACT로 켜둡니다.
        var chatState by remember { 
            mutableStateOf(
                ChatRoomState(
                    roomId = "room123",
                    mode = ChatRoomMode.CONTRACT,
                    selectedContract = ChatPreviewData.dummyContracts[0] // ChatPreviewData 연동
                )
            ) 
        }

        // 바텀 시트 표시 상태
        var isDocumentSheetVisible by remember { mutableStateOf(false) }

        ChatRoomScreen(
            state = chatState,
            onBackClick = {},
            onModeChange = { newMode ->
                // 상단 배너의 X 버튼을 누르면 NORMAL 모드로 원복됨 (인터랙티브 모드에서 동작)
                if (newMode == ChatRoomMode.NORMAL) {
                    chatState = chatState.copy(mode = ChatRoomMode.NORMAL, selectedContract = null)
                }
            },
            messageListContent = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. A(나)가 보낸 초기 초안
                    item {
                        DraftRequestCard(
                            message = DraftRequestMessage(
                                id = "flow1",
                                sender = MessageSender.ME, // A의 시점
                                timestamp = LocalDateTime.now(),
                                contractTitle = "100만원 소액 대출 건",
                                actionStatus = ActionStatus.REJECTED // 상대방이 수정을 제안한 상태
                            ),
                            onAcceptClick = {},
                            onProposeClick = {},
                            onViewDetailsClick = { isDocumentSheetVisible = true }
                        )
                    }

                    // 2. B의 수정 제안 시스템 알림 (중앙)
                    item {
                        SystemNoticeCard(
                            message = SystemNoticeMessage(
                                id = "notice1",
                                timestamp = LocalDateTime.now(),
                                text = "김철수님이 [100만원 소액 대출 건] 초안 수정을 제안했습니다."
                            )
                        )
                    }

                    // 3. B가 보낸 수정 제안 브릿지 카드
                    item {
                        ProposalRequestCard(
                            message = ProposalRequestMessage(
                                id = "flow2",
                                sender = MessageSender.OTHER,
                                timestamp = LocalDateTime.now(),
                                contractTitle = "100만원 소액 대출 건",
                                actionStatus = if (chatState.mode == ChatRoomMode.CONTRACT) ActionStatus.COMPLETED else ActionStatus.PENDING
                            ),
                            onAcceptClick = {
                                // [!수락 버튼 클릭 시 모드 전환!]
                                chatState = chatState.copy(
                                    mode = ChatRoomMode.CONTRACT,
                                    selectedContract = ChatPreviewData.dummyContracts[0]
                                )
                            },
                            onRejectClick = {}
                        )
                    }

                    // 4. 모듈 돌입 시 챗봇 개입
                    item {
                        SystemNoticeCard(
                            message = SystemNoticeMessage(
                                id = "notice2",
                                timestamp = LocalDateTime.now(),
                                text = ChatSystemMessages.AI_ASSISTANT_GREETING
                            )
                        )
                    }

                    // 5. 상대방(B) 채팅 입력
                    item {
                        BaseChatCard(sender = MessageSender.OTHER) {
                            Text(
                                text = "우리 저번에 말한 대로 매달 10일에 이자 주기로 한 거 특약에 넣어줘. 기한은 두 달 더 연장하고.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // 6. 나(A)의 합의
                    item {
                        BaseChatCard(sender = MessageSender.ME) {
                            Text(
                                text = "그래 알았어. 대신 연체 이자는 5%로 하자.",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // 7. 최종 수정안 카드 배출
                    item {
                        ConditionProposalCard(
                            message = ConditionProposalMessage(
                                id = "flow3",
                                sender = MessageSender.SYSTEM,
                                timestamp = LocalDateTime.now(),
                                modifications = listOf(
                                    "상환 기한: 2026.05.31 -> 2026.07.31 (2개월 연장)",
                                    "특약 추가: 매월 10일 이자 지급 의무",
                                    "특약 추가: 연체 이자 5% 적용"
                                ),
                                actionStatus = ActionStatus.PENDING
                            ),
                            onAcceptClick = {},
                            onRejectClick = {} // 직접 수정하기 매핑 예정
                        )
                    }
                    
                    // 8. 시스템 가이드 및 종료 안내
                    item {
                        SystemNoticeCard(
                            message = SystemNoticeMessage(
                                id = "notice3",
                                timestamp = LocalDateTime.now(),
                                text = ChatSystemMessages.AI_ASSISTANT_GUIDE
                            )
                        )
                    }
                }
            } // end messageListContent
        ) // end ChatRoomScreen

        // 차용증 전문 보기 전체 화면 연동
        if (isDocumentSheetVisible) {
            ContractDocumentFullScreen(
                contractTitle = "100만원 소액 대출 건",
                contractDetail = null,
                onDismissRequest = { isDocumentSheetVisible = false },
                onAccept = { isDocumentSheetVisible = false }
            )
        }
    }
}

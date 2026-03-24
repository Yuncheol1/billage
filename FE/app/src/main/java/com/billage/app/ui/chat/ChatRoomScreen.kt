package com.billage.app.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billage.app.ui.chat.components.ChatBottomInputBar
import com.billage.app.ui.chat.components.ChatTopBar
import com.billage.app.ui.chat.components.PinnedContractBanner
import com.billage.app.ui.chat.models.ChatRoomState
import com.billage.app.ui.chat.models.ChatRoomMode
import com.billage.app.data.remote.api.ContractListResponse
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import com.billage.app.ui.chat.models.ContractSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    state: ChatRoomState,
    contracts: List<ContractListResponse> = emptyList(),
    onFetchContracts: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onModeChange: (ChatRoomMode) -> Unit = {}, // 명시적 모드 종료(배너 닫기) 등을 처리
    onSendMessage: (String) -> Unit = {},
    onAddMenuClick: () -> Unit = {},
    onRequestAiProposal: () -> Unit = {},
    onContractSelectForModify: (ContractListResponse) -> Unit = {},
    onCommitProposal: () -> Unit = {},
    onCancelProposal: () -> Unit = {},
    onDirectEditClick: (Int) -> Unit = {},
    messageListContent: @Composable () -> Unit
) {
    // 입력창 텍스트 관리
    var inputText by remember { mutableStateOf("") }
    // 액션 메뉴(바텀시트) 표시 여부 관리
    var showActionSheet by remember { mutableStateOf(false) }
    // 바텀시트 내부 상태 관리 (MENU, MANUAL_EDIT_PICKER)
    var actionSheetState by remember { mutableStateOf("MENU") }
    // 제안서 모달 창 표시 여부
    var showProposalModal by remember { mutableStateOf(false) }
    // AI 차용증 선택 다이얼로그 표시 여부
    var showAiSelectDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ChatTopBar(
                title = state.roomName,
                isNegotiationMode = state.mode == ChatRoomMode.CONTRACT,
                onNegotiationToggle = { isOn ->
                    if (isOn) {
                        onFetchContracts()
                        showAiSelectDialog = true
                    } else {
                        onModeChange(ChatRoomMode.NORMAL)
                    }
                },
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            ChatBottomInputBar(
                mode = state.mode,
                inputText = inputText,
                onValueChange = { 
                    if (it.length <= 255) {
                        inputText = it 
                    }
                },
                onSendClick = {
                    if (inputText.isNotBlank() && inputText.length <= 255) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                onAddClick = {
                    actionSheetState = "MENU"
                    showActionSheet = true
                    onAddMenuClick() // 기존 콜백도 유지
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 양측 수락 시 상단에 고정되는 전자서명 안내 배너
            com.billage.app.ui.chat.components.SignatureBanner(
                isVisible = state.isMeAccepted && state.isOpponentAccepted,
                onSignClick = {
                    // TODO: 전자서명 화면으로 이동
                    // appNavController.navigateToSignature(state.selectedContract?.contractSeq ?: -1)
                }
            )

            // 계약 모드일 경우 상단에 고정되는 핀 배너
            PinnedContractBanner(
                selectedContract = state.selectedContract,
                onCloseClick = { onModeChange(ChatRoomMode.NORMAL) },
                onGenerateDraftClick = {
                    onRequestAiProposal()
                }
            )
            
            // 메인 메시지 영역
            androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                messageListContent()

                // MOCK AI 제안 도착 카드 (채팅방 하단 고정 -> 나중에 LazyColumn 내부로 이동 완료)
            }
        }
    }

    // '+' 버튼 클릭 시 뜨는 바텀 시트 메뉴 (차용증 조회/수정 등)
    if (showActionSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (actionSheetState == "MENU") {
                    androidx.compose.material3.Text(
                        text = "채팅 액션 메뉴",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 차용증 직접 작성/수정 버튼 (프론트/백엔드 폼 이동)
                    androidx.compose.material3.ListItem(
                        headlineContent = { androidx.compose.material3.Text("차용증 직접 작성/수정하기") },
                        supportingContent = { androidx.compose.material3.Text("입력 폼 화면으로 이동하여 항목을 직접 변경합니다.") },
                        leadingContent = {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "차용증 수정",
                                tint = com.billage.app.ui.theme.BillagePrimary
                            )
                        },
                        modifier = Modifier.clickable {
                            onFetchContracts()
                            actionSheetState = "MANUAL_EDIT_PICKER"
                        }
                    )
                    
                    // 송금하기 버튼
                    androidx.compose.material3.ListItem(
                        headlineContent = { androidx.compose.material3.Text("송금 하기") },
                        supportingContent = { androidx.compose.material3.Text("상대방에게 돈을 송금합니다.") },
                        leadingContent = {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "송금",
                                tint = com.billage.app.ui.theme.BillagePrimary
                            )
                        },
                        modifier = Modifier.clickable {
                            showActionSheet = false
                            // TODO: 송금 화면 이동
                        }
                    )
                } else if (actionSheetState == "MANUAL_EDIT_PICKER") {
                    // 뒤로가기 버튼 + 헤더
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기",
                            modifier = Modifier
                                .clickable { actionSheetState = "MENU" }
                                .padding(end = 8.dp)
                        )
                        androidx.compose.material3.Text(
                            text = "직접 수정할 차용증 선택",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    }

                    val editableContracts = contracts.filter { it.status == "DRAFT" || it.status == "NEGOTIATING" }
                    
                    if (editableContracts.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 8.dp)
                        ) {
                            items(editableContracts) { contract ->
                                Card(
                                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, com.billage.app.ui.theme.BillageDivider),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .clickable {
                                            showActionSheet = false
                                            actionSheetState = "MENU"
                                            onDirectEditClick(contract.contractSeq)
                                        }
                                ) {
                                    androidx.compose.foundation.layout.Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(com.billage.app.ui.theme.BillageBackground, androidx.compose.foundation.shape.CircleShape)
                                                .padding(8.dp),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            androidx.compose.material3.Text("📝", fontSize = 20.sp)
                                        }
                                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            androidx.compose.material3.Text(
                                                text = "계약 #${contract.contractSeq}",
                                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = com.billage.app.ui.theme.BillageTextPrimary
                                            )
                                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                                            androidx.compose.material3.Text(
                                                text = "상대방: ${contract.counterpartyName ?: "미상"}",
                                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                                color = com.billage.app.ui.theme.BillageTextSecondary
                                            )
                                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(2.dp))
                                            androidx.compose.material3.Text(
                                                text = "원금: ${contract.principalAmount?.toInt() ?: 0}원",
                                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                                color = com.billage.app.ui.theme.BillagePrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        androidx.compose.material3.Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "수정 아이콘",
                                            tint = com.billage.app.ui.theme.BillageDivider,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "직접 수정할 수 있는 차용증이 없습니다.\n(진행 중인 계약만 수정 가능합니다)",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProposalModal && state.draftContractDetail != null) {
        com.billage.app.ui.chat.components.ContractDocumentFullScreen(
            contractTitle = "계약 #${state.selectedContract?.contractId ?: ""} 수정 제안서",
            contractDetail = state.draftContractDetail!!,
            isMeAccepted = state.isMeAccepted,
            isOpponentAccepted = state.isOpponentAccepted,
            onDismissRequest = { showProposalModal = false },
            onAccept = {
                showProposalModal = false
                onCommitProposal()
            }
        )
    }

    if (showAiSelectDialog) {
        com.billage.app.ui.chat.components.AiContractSelectDialog(
            contracts = contracts,
            onDismissRequest = { 
                showAiSelectDialog = false
                // 사용자가 다이얼로그를 그냥 닫으면 네고시에이션 모드도 다시 끔
                onModeChange(ChatRoomMode.NORMAL) 
            },
            onContractSelect = { contract ->
                showAiSelectDialog = false
                onContractSelectForModify(contract)
            }
        )
    }
}

@Composable
fun ProposalNotificationCard(
    contractSummary: ContractSummary?,
    onDetailClick: () -> Unit
) {
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.White
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.billage.app.ui.theme.BillageDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // 헤더
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    text = "📜 [계약 #${contractSummary?.contractId ?: ""}] 수정 제안 도착",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = com.billage.app.ui.theme.BillageTextPrimary
                )
            }
            androidx.compose.material3.HorizontalDivider(color = com.billage.app.ui.theme.BillageDivider)
            
            // 본문
            Column(modifier = Modifier.padding(16.dp)) {
                androidx.compose.material3.Text(
                    text = "📝 문서 병합 완료",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = com.billage.app.ui.theme.BillageTextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.material3.Text(
                    text = "채팅 기반으로 합의된 내용을 문서에 반영했습니다.\n\n자세히 보기를 클릭해 확인해주세요.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = com.billage.app.ui.theme.BillageTextSecondary
                )
            }
            androidx.compose.material3.HorizontalDivider(color = com.billage.app.ui.theme.BillageDivider)
            
            // 버튼
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDetailClick() }
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "🔍 제안서 자세히 보기",
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = com.billage.app.ui.theme.BillagePrimary
                )
            }
        }
    }
}

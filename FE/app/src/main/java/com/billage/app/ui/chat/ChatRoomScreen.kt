package com.billage.app.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
    messageListContent: @Composable () -> Unit
) {
    // 입력창 텍스트 관리
    var inputText by remember { mutableStateOf("") }
    // 액션 메뉴(바텀시트) 표시 여부 관리
    var showActionSheet by remember { mutableStateOf(false) }
    // 바텀시트 내부 상태 관리 (MENU, CONTRACTS)
    var actionSheetState by remember { mutableStateOf("MENU") }

    Scaffold(
        topBar = {
            ChatTopBar(
                title = state.roomName, // state에서 가져온 방 제목 표시
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
            // 계약 모드일 경우 상단에 고정되는 핀 배너 (퀄리티 업)
            PinnedContractBanner(
                selectedContract = state.selectedContract,
                onCloseClick = {
                    // 배너 우측 닫기(X) 버튼 누르면 일반 대화 모드로 원복
                    onModeChange(ChatRoomMode.NORMAL)
                }
            )
            
            // 메인 메시지 영역
            messageListContent()
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

                    // 차용증 조회 및 수정 버튼
                    androidx.compose.material3.ListItem(
                        headlineContent = { androidx.compose.material3.Text("차용증 조회 및 수정") },
                        supportingContent = { androidx.compose.material3.Text("진행 중인 차용증 목록을 확인합니다.") },
                        leadingContent = {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "차용증 수정",
                                tint = com.billage.app.ui.theme.BillagePrimary
                            )
                        },
                        modifier = Modifier.clickable {
                            onFetchContracts()
                            actionSheetState = "CONTRACTS"
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
                } else if (actionSheetState == "CONTRACTS") {
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
                            text = "내 활성 차용증 목록",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    }

                    if (contracts.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 8.dp)
                        ) {
                            items(contracts) { contract ->
                                androidx.compose.material3.ListItem(
                                    headlineContent = { androidx.compose.material3.Text("차용증 #${contract.contractSeq} (${contract.contractId ?: "ID 없음"})") },
                                    supportingContent = { 
                                        androidx.compose.material3.Text(
                                            "상대: ${contract.counterpartyName ?: "미상"} / 원금: ${contract.principalAmount ?: 0}원 / 방식: ${contract.repaymentMethod ?: "없음"}"
                                        ) 
                                    },
                                    modifier = Modifier.clickable {
                                        showActionSheet = false
                                        actionSheetState = "MENU"
                                        // TODO: 해당 contract.contractSeq를 수정 화면으로 넘기기
                                    }
                                )
                            }
                        }
                    } else {
                        androidx.compose.material3.Text(
                            text = "불러오는 중이거나 차용증이 없습니다.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

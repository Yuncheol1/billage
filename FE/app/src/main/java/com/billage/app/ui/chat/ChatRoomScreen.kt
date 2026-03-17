package com.billage.app.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.billage.app.ui.chat.components.ChatBottomInputBar
import com.billage.app.ui.chat.components.ChatTopBar
import com.billage.app.ui.chat.components.PinnedContractBanner
import com.billage.app.ui.chat.models.ChatRoomState
import com.billage.app.ui.chat.models.ChatRoomMode

@Composable
fun ChatRoomScreen(
    state: ChatRoomState,
    onBackClick: () -> Unit = {},
    onModeChange: (ChatRoomMode) -> Unit = {}, // 명시적 모드 종료(배너 닫기) 등을 처리
    onSendMessage: (String) -> Unit = {},
    onAddMenuClick: () -> Unit = {},
    messageListContent: @Composable () -> Unit
) {
    // 입력창 텍스트 관리
    var inputText by remember { mutableStateOf("") }

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
                onValueChange = { inputText = it },
                onSendClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                onAddClick = onAddMenuClick
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
}

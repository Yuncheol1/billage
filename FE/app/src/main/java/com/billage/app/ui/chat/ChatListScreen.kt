package com.billage.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.common.BottomBar
import com.billage.app.ui.theme.*

data class ChatPreviewItem(
    val id: String,
    val name: String,
    val avatar: String, // Emoji string for now
    val lastMessage: String,
    val timestamp: String,
    val unread: Int,
    val hasContract: Boolean
)

val dummyChatList = listOf(
    ChatPreviewItem("1", "김철수", "👨", "네 알겠습니다!", "방금", 0, true),
    ChatPreviewItem("2", "이영희", "👩", "고마워요~", "10분 전", 0, true),
    ChatPreviewItem("3", "박민수", "👨‍💼", "오늘 저녁에 통화 가능해?", "1시간 전", 2, false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatRoomClick: (String) -> Unit,
    onHomeClick: () -> Unit,
    onAddContractClick: () -> Unit
) {
    Scaffold(
        containerColor = BillageBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BillageSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "메시지",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BillageTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "대화 상대와 안전하게 거래하세요",
                    fontSize = 13.sp,
                    color = BillageTextSecondary
                )
            }
        },
        bottomBar = {
            BottomBar(
                currentRoute = "chat_list",
                onAddContractClick = onAddContractClick,
                onHomeClick = onHomeClick,
                onChatClick = {} // We are already here
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(dummyChatList) { chat ->
                ChatListItem(
                    chat = chat,
                    onClick = { onChatRoomClick(chat.id) }
                )
            }
            if (dummyChatList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "💬", fontSize = 60.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "아직 대화가 없어요",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BillageTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "거래하고 싶은 친구에게 메시지를 보내보세요",
                            fontSize = 14.sp,
                            color = BillageTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: ChatPreviewItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(BillageSurface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BillagePrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Text(text = chat.avatar, fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                    if (chat.hasContract) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BillagePrimaryLight)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "계약중",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BillagePrimary
                            )
                        }
                    }
                }
                Text(
                    text = chat.timestamp,
                    fontSize = 12.sp,
                    color = BillageTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessage,
                    fontSize = 14.sp,
                    color = BillageTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (chat.unread > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(BillagePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unread.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider(color = BillageDivider.copy(alpha=0.5f))
}

@Preview(showBackground = true)
@Composable
fun ChatListScreenPreview() {
    BillageTheme(dynamicColor = false) {
        ChatListScreen(
            onChatRoomClick = {},
            onHomeClick = {},
            onAddContractClick = {}
        )
    }
}

package com.billage.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.LaunchedEffect
import com.billage.app.ui.chat.security.SecurityTestBed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.common.BottomBar
import com.billage.app.ui.common.BottomBarTab
import com.billage.app.ui.common.MenuProfileState
import com.billage.app.ui.home.HomeSideMenu
import com.billage.app.ui.theme.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlin.collections.isNotEmpty

data class ChatPreviewItem(
    val id: String,
    val name: String,
    val avatar: String, // Emoji string for now
    val lastMessage: String,
    val timestamp: String,
    val unread: Int,
    val hasContract: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel = hiltViewModel(),
    onChatRoomClick: (String, String) -> Unit,
    onHomeClick: () -> Unit,
    onAddContractClick: (com.billage.app.ui.contract.create.ContractRole) -> Unit,
    onContractClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    menuProfileState: MenuProfileState = MenuProfileState()
) {
    val rooms by viewModel.rooms.collectAsState()
    val userSearchResults by viewModel.userSearchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val myUserSeq = viewModel.myUserSeq
    var showMenu by remember { mutableStateOf(false) }
    val logoutUiState = com.billage.app.ui.common.rememberLogoutUiState(onLoggedOut = onLogoutClick)
    val dismissInteraction = remember { MutableInteractionSource() }
    
    // 화면 진입 시마다 목록 새로고침
    LaunchedEffect(Unit) {
        viewModel.fetchRooms()
    }

    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    if (showCreateRoomDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            title = { Text("새 채팅방 만들기", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        label = { Text("상대방 이메일") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (userSearchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                            colors = CardDefaults.cardColors(containerColor = BillageBackground),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            LazyColumn {
                                items(userSearchResults) { user ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.onSearchQueryChange(user.email) }
                                            .padding(12.dp)
                                    ) {
                                        Text(user.realName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(user.email, fontSize = 12.sp, color = BillageTextSecondary)
                                    }
                                    HorizontalDivider(color = BillageDivider.copy(alpha=0.5f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        if (searchQuery.isNotBlank()) {
                            isCreating = true
                            viewModel.createRoomByEmail(
                                targetEmail = searchQuery,
                                onSuccess = { id, opponent -> 
                                    isCreating = false
                                    viewModel.onSearchQueryChange("")
                                    onChatRoomClick(id, opponent)
                                },
                                onError = {
                                    isCreating = false
                                }
                            )
                        }
                    },
                    enabled = !isCreating && searchQuery.isNotBlank()
                ) {
                    Text(if (isCreating) "생성 중..." else "만들기", color = BillagePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoomDialog = false }) {
                    Text("취소", color = BillageTextSecondary)
                }
            },
            containerColor = BillageSurface,
            titleContentColor = BillageTextPrimary,
            textContentColor = BillageTextPrimary
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "메시지",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = BillageTextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.fetchRooms() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "새로고침", tint = BillageTextPrimary)
                            }
                            IconButton(onClick = { showCreateRoomDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "채팅방 추가", tint = BillageTextPrimary)
                            }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "메뉴", tint = BillageTextPrimary)
                            }
                        }
                    }
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
                    selectedTab = if (showMenu) BottomBarTab.Menu else BottomBarTab.ChatList,
                    onAddContractClick = { onAddContractClick(com.billage.app.ui.contract.create.ContractRole.CREDITOR) }, // BottomBar는 role을 전달하지 않으므로 기본값(또는 바텀시트 동작) 처리
                    onHomeClick = onHomeClick,
                    onChatClick = {}, // We are already here
                    onContractClick = onContractClick,
                    onMenuClick = { showMenu = true }
                )
            }
        ) { paddingValues ->
            val context = LocalContext.current
            var testResult by remember { mutableStateOf(SecurityTestBed.getDeviceSecuritySpec(context)) }
            var showEnrollDialog by remember { mutableStateOf(false) }

            if (showEnrollDialog) {
                AlertDialog(
                    onDismissRequest = { showEnrollDialog = false },
                    title = { Text("화면 잠금 설정 필요", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("전자서명(TEE/StrongBox)을 안전하게 저장하기 위해 기기에 화면 잠금(PIN, 패턴, 지문 등)이 필수로 설정되어 있어야 합니다.\n\n설정 앱에서 화면 잠금을 등록한 후 다시 시도해주세요.")
                    },
                    confirmButton = {
                        TextButton(onClick = { showEnrollDialog = false }) {
                            Text("확인", color = BillagePrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = BillageSurface,
                    titleContentColor = BillageTextPrimary,
                    textContentColor = BillageTextPrimary
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(rooms) { room ->
                // Map ChatRoomRes to ChatPreviewItem for UI
                // 내 UserSeq가 아닌 참여자의 이름을 찾아서 표시 (없으면 알 수 없음)
                val opponentName = room.participants.firstOrNull { it.userSeq != myUserSeq }?.name ?: "알 수 없음"
                val chat = ChatPreviewItem(
                    id = room.id,
                    name = opponentName,
                    avatar = "👤",
                    lastMessage = room.lastMessage ?: "",
                    timestamp = room.lastModifiedAt ?: "",
                    unread = room.unreadCount.toInt(),
                    hasContract = false
                )
                var showLeaveDialog by remember { mutableStateOf(false) }
                if (showLeaveDialog) {
                    AlertDialog(
                        onDismissRequest = { showLeaveDialog = false },
                        title = { Text("채팅방 나가기", fontWeight = FontWeight.Bold) },
                        text = { Text("정말 채팅방을 나가시겠습니까?") },
                        confirmButton = {
                            TextButton(onClick = { 
                                showLeaveDialog = false
                                viewModel.leaveRoom(room.id)
                            }) {
                                Text("나가기", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLeaveDialog = false }) {
                                Text("취소", color = BillageTextSecondary)
                            }
                        },
                        containerColor = BillageSurface,
                        titleContentColor = BillageTextPrimary,
                        textContentColor = BillageTextPrimary
                    )
                }

                    ChatListItem(
                        chat = chat,
                        onClick = { onChatRoomClick(chat.id, opponentName) },
                        onLeaveClick = { showLeaveDialog = true }
                    )
                }
                if (rooms.isEmpty()) {
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

        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f))
                    .clickable(
                        interactionSource = dismissInteraction,
                        indication = null
                    ) { showMenu = false }
            )
        }

        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                HomeSideMenu(
                    userName = menuProfileState.realName.ifBlank { "빌리지" },
                    email = menuProfileState.email,
                    isLoggingOut = logoutUiState.isLoggingOut,
                    onClose = { showMenu = false },
                    onProfileClick = { showMenu = false },
                    onContractMenuClick = {
                        showMenu = false
                        onContractClick()
                    },
                    onReportMenuClick = { showMenu = false },
                    onSettingsClick = {
                        showMenu = false
                        onSettingsClick()
                    },
                    onSupportClick = { showMenu = false },
                    onTermsClick = { showMenu = false },
                    onPrivacyClick = { showMenu = false },
                    onLogoutClick = {
                        showMenu = false
                        logoutUiState.onLogoutClick()
                    }
                )
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: ChatPreviewItem,
    onClick: () -> Unit,
    onLeaveClick: () -> Unit
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
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onLeaveClick, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "나가기", tint = BillageTextSecondary)
        }
    }
    HorizontalDivider(color = BillageDivider.copy(alpha=0.5f))
}

@Preview(showBackground = true)
@Composable
fun ChatListScreenPreview() {
    BillageTheme(dynamicColor = false) {
        ChatListScreen(
            onChatRoomClick = { _, _ -> },
            onHomeClick = {},
            onAddContractClick = {}
        )
    }
}

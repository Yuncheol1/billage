package com.billage.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.billage.app.ui.auth.ConditionsOfUseScreen
import com.billage.app.ui.auth.LoginScreen
import com.billage.app.ui.auth.MypageScreen
import com.billage.app.ui.contract.ContractScreen
import com.billage.app.ui.home.HomeScreen
import com.billage.app.ui.chat.ChatRoomScreen
import com.billage.app.ui.chat.models.ChatRoomState
import com.billage.app.ui.chat.models.MessageSender
import com.billage.app.ui.chat.components.cards.BaseChatCard
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import androidx.compose.ui.Alignment

fun NavGraphBuilder.appNavGraph(
    appNavController: AppNavController
) {
    composable(route = AppDestination.Login.route) {
        LoginScreen(
            onDevBypassClick = { appNavController.navigateToConditionsOfUse() }
        )
    }

    composable(route = AppDestination.ConditionsOfUse.route) {
        ConditionsOfUseScreen(
            onBackClick = { appNavController.popBackStack() },
            onAgreeClick = { appNavController.navigateToHome() }
        )
    }

    composable(route = AppDestination.Home.route) {
        HomeScreen(
            onContractClick = { appNavController.navigateToMypage() },
            onNotificationClick = { /* TODO: 알림 화면으로 이동 */ },
            onAddContractClick = { appNavController.navigateToContract() },
            onActivityClick = { /* TODO: 전체 내역 화면으로 이동 */ },
            onChatClick = { appNavController.navigateToChatList() },
            onMypageClick = { appNavController.navigateToMypage() },
            onLogoutClick = { appNavController.navigateToLogin(clearBackStack = true) }
        )
    }

    composable(route = AppDestination.Contract.route) {
        ContractScreen(
            onNavigateBack = { appNavController.popBackStack() },
            onContractComplete = { /* TODO: 전송 완료 대기 화면으로 이동 */ }
        )
    }

    composable(route = AppDestination.ChatList.route) {
        com.billage.app.ui.chat.ChatListScreen(
            onChatRoomClick = { roomId, opponentName -> appNavController.navigateToChatRoom(roomId, opponentName) },
            onHomeClick = { appNavController.navigateToHome() },
            onAddContractClick = { appNavController.navigateToContract() },
            onContractClick = { appNavController.navigateToMypage() }
        )
    }

    composable(route = AppDestination.ChatRoom.route + "/{roomId}/{opponentName}") { backStackEntry ->
        val roomId = backStackEntry.arguments?.getString("roomId") ?: "100"
        val opponentName = backStackEntry.arguments?.getString("opponentName") ?: "알 수 없음"
        val viewModel: com.billage.app.ui.chat.ChatRoomViewModel = hiltViewModel()
        
        LaunchedEffect(roomId) {
            viewModel.joinRoom(roomId)
        }

        val messages by viewModel.messages.collectAsState()

        com.billage.app.ui.chat.ChatRoomScreen(
            state = ChatRoomState(
                roomId = roomId,
                roomName = opponentName
            ),
            onBackClick = { appNavController.popBackStack() },
            onSendMessage = { content -> viewModel.sendMessage(content) },
            messageListContent = { 
                LazyColumn(
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages.asReversed()) { msg ->
                        val isMe = msg.senderSeq == viewModel.myUserSeq
                        if (msg.isContractMsg) {
                            com.billage.app.ui.chat.components.cards.SystemNoticeCard(
                                message = com.billage.app.ui.chat.models.SystemNoticeMessage(
                                    id = msg.id,
                                    timestamp = java.time.LocalDateTime.now(),
                                    text = msg.content
                                )
                            )
                        } else {
                            BaseChatCard(
                                sender = if (isMe) MessageSender.ME else MessageSender.OTHER
                            ) {
                                Column {
                                    if (!isMe) {
                                        Text(
                                            text = msg.senderName, // 백엔드에서 내려온 진짜 상대방 이름 표시
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BillageTextSecondary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = msg.content,
                                        color = if (isMe) BillageTextPrimary else BillageTextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = msg.createdAt.substringAfter("T").substringBefore("."),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = (if (isMe) BillageTextPrimary else BillageTextSecondary).copy(alpha = 0.5f),
                                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // fallback for route without args just in case
    composable(route = AppDestination.ChatRoom.route) {
        val viewModel: com.billage.app.ui.chat.ChatRoomViewModel = hiltViewModel()
        LaunchedEffect(Unit) { viewModel.joinRoom("100") }
        val messages by viewModel.messages.collectAsState()

        com.billage.app.ui.chat.ChatRoomScreen(
            state = ChatRoomState(
                roomId = "100",
                roomName = "알 수 없음"
            ),
            onBackClick = { appNavController.popBackStack() },
            onSendMessage = { content -> viewModel.sendMessage(content) },
            messageListContent = { 
                LazyColumn(
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages.asReversed()) { msg ->
                        val isMe = msg.senderSeq == viewModel.myUserSeq
                        if (msg.isContractMsg) {
                            com.billage.app.ui.chat.components.cards.SystemNoticeCard(
                                message = com.billage.app.ui.chat.models.SystemNoticeMessage(
                                    id = msg.id,
                                    timestamp = java.time.LocalDateTime.now(),
                                    text = msg.content
                                )
                            )
                        } else {
                            BaseChatCard(
                                sender = if (isMe) MessageSender.ME else MessageSender.OTHER
                            ) {
                                Column {
                                    if (!isMe) {
                                        Text(
                                            text = msg.senderName, // 백엔드에서 내려온 진짜 상대방 이름 표시
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BillageTextSecondary,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = msg.content,
                                        color = if (isMe) BillageTextPrimary else BillageTextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = msg.createdAt.substringAfter("T").substringBefore("."),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = (if (isMe) BillageTextPrimary else BillageTextSecondary).copy(alpha = 0.5f),
                                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    composable(route = AppDestination.Mypage.route) {
        MypageScreen(
            onNavigateHome = { appNavController.navigateToHome() },
            onNotificationClick = { /* TODO: 알림 화면으로 이동 */ },
            onAddContractClick = { appNavController.navigateToContract() },
            onChatClick = { appNavController.navigateToChatList() },
            onLogoutClick = { appNavController.navigateToLogin(clearBackStack = true) }
        )
    }
}

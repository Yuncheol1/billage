package com.billage.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.billage.app.ui.auth.ConditionsOfUseScreen
import com.billage.app.ui.auth.LoginScreen
import com.billage.app.ui.contract.ContractListScreen
import com.billage.app.ui.contract.ContractScreen
import com.billage.app.ui.home.HomeScreen
import com.billage.app.ui.common.MenuProfileViewModel
import com.billage.app.ui.chat.ChatRoomScreen
import com.billage.app.ui.chat.models.ChatRoomState
import com.billage.app.ui.chat.models.MessageSender
import com.billage.app.ui.chat.models.AiProposalPayload
import com.billage.app.ui.chat.components.cards.BaseChatCard
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.billage.app.ui.contract.ContractCompleteScreen
import com.billage.app.ui.contract.ContractRole
import com.billage.app.ui.contract.ContractSpecialTermsScreen
import com.billage.app.ui.contract.ContractViewModel
import com.billage.app.ui.settings.WithdrawalSettingsScreen
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel

fun NavGraphBuilder.appNavGraph(
    appNavController: AppNavController
) {
    composable(route = AppDestination.Login.route) {
        LoginScreen(
            onDevBypassClick = { appNavController.navigateToHome(clearBackStack = true) },
            onBiometricLoginSuccess = { userState ->
                if (userState.equals("PENDING", ignoreCase = true)) {
                    appNavController.navigateToConditionsOfUse(clearBackStack = true)
                } else {
                    appNavController.navigateToHome(clearBackStack = true)
                }
            }
        )
    }

    composable(route = AppDestination.ConditionsOfUse.route) {
        ConditionsOfUseScreen(
            onBackClick = { appNavController.popBackStack() },
            onAgreeClick = { appNavController.navigateToHome() }
        )
    }

    composable(route = AppDestination.Home.route) {
        val menuProfileViewModel: MenuProfileViewModel = hiltViewModel()
        val menuProfileState by menuProfileViewModel.uiState.collectAsState()
        HomeScreen(
            onContractClick = { appNavController.navigateToMypage() },
            onNotificationClick = { /* TODO: 알림 화면으로 이동 */ },
            onAddContractClick = { role -> appNavController.navigateToContract(role) },
            onActivityClick = { /* TODO: 전체 내역 화면으로 이동 */ },
            onChatClick = { appNavController.navigateToChatList() },
            onMypageClick = { appNavController.navigateToMypage() },
            onSettingsClick = { appNavController.navigateToSettings() },
            onLogoutClick = { appNavController.navigateToLogin(clearBackStack = true) },
            menuProfileState = menuProfileState
        )
    }

    composable(route = AppDestination.Contract.routeWithArg) { backStackEntry ->
        val role = backStackEntry.arguments?.getString("role")
            ?.let { ContractRole.valueOf(it) } ?: ContractRole.CREDITOR
        ContractScreen(
            onNavigateBack = { appNavController.popBackStack() },
            onContractComplete = { contractSeq ->  // ← contractSeq 받아서 전달
                appNavController.navigateToSpecialTerms(contractSeq)
            },
            role = role
        )
    }

    composable(route = AppDestination.ChatList.route) {
        val menuProfileViewModel: MenuProfileViewModel = hiltViewModel()
        val menuProfileState by menuProfileViewModel.uiState.collectAsState()
        com.billage.app.ui.chat.ChatListScreen(
            onChatRoomClick = { roomId, opponentName -> appNavController.navigateToChatRoom(roomId, opponentName) },
            onHomeClick = { appNavController.navigateToHome() },
            onAddContractClick = { role -> appNavController.navigateToContract(role) },
            onContractClick = { appNavController.navigateToMypage() },
            onSettingsClick = { appNavController.navigateToSettings() },
            onLogoutClick = { appNavController.navigateToLogin(clearBackStack = true) },
            menuProfileState = menuProfileState
        )
    }

    composable(route = AppDestination.ChatRoom.route + "/{roomId}/{opponentName}") { backStackEntry ->
        val roomId = backStackEntry.arguments?.getString("roomId") ?: "100"
        val opponentName = backStackEntry.arguments?.getString("opponentName") ?: "알 수 없음"
        val viewModel: com.billage.app.ui.chat.ChatRoomViewModel = hiltViewModel()

        LaunchedEffect(roomId) {
            viewModel.joinRoom(roomId, opponentName)
        }

        val messages by viewModel.messages.collectAsState()
        val contracts by viewModel.contracts.collectAsState()
        val chatRoomState by viewModel.chatRoomState.collectAsState()
        val localCards by viewModel.localCards.collectAsState()

        var showProposalDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        // 제안서 다이얼로그: localCards에 AiProposalMessage 등록 후 showProposalDialog=true 하면 오픈
        if (showProposalDialog && chatRoomState.draftContractDetail != null) {
            com.billage.app.ui.chat.components.ContractDocumentFullScreen(
                contractTitle = "계약 #${chatRoomState.selectedContract?.contractId ?: ""} 수정 제안서",
                contractDetail = chatRoomState.draftContractDetail,
                onDismissRequest = { showProposalDialog = false },
                onAccept = {
                    showProposalDialog = false
                    viewModel.submitProposal()
                }
            )
        }

        ChatRoomScreen(
            state = chatRoomState,
            onBackClick = { appNavController.popBackStack() },
            contracts = contracts,
            onFetchContracts = { viewModel.fetchContracts() },
            onModeChange = { mode ->
                if (mode == com.billage.app.ui.chat.models.ChatRoomMode.NORMAL) {
                    viewModel.exitModificationMode()
                }
            },
            onAddMenuClick = {},
            onRequestAiProposal = { viewModel.requestAiProposal() },
            onContractSelectForModify = { viewModel.setModificationMode(it) },
            onCommitProposal = { viewModel.submitProposal() },
            onCancelProposal = { viewModel.cancelProposal() },
            onSendMessage = { content -> viewModel.sendMessage(content) },
            onDirectEditClick = { contractId -> appNavController.navigateToContractEdit(contractId) },
            messageListContent = {
                LazyColumn(
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages.asReversed()) { msg ->
                        val isMe = msg.senderSeq == viewModel.myUserSeq
                        when (msg.msgType) {
                            "AI_PROPOSAL" -> {
                                // JSON content 파싱 → AiProposalCard 렌더링
                                val parsed = try {
                                    kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                                        .decodeFromString<AiProposalPayload>(msg.content)
                                } catch (e: Exception) {
                                    null
                                }
                                if (parsed != null) {
                                    com.billage.app.ui.chat.components.cards.AiProposalCard(
                                        message = com.billage.app.ui.chat.models.AiProposalMessage(
                                            id = msg.id,
                                            sender = if (isMe) MessageSender.ME else MessageSender.OTHER,
                                            timestamp = java.time.LocalDateTime.now(),
                                            contractTitle = parsed.contractTitle,
                                            originalInterestRate = parsed.originalInterestRate,
                                            proposedInterestRate = parsed.proposedInterestRate,
                                            modifications = parsed.modifications
                                        ),
                                        onViewDetailsClick = {
                                            // 카드 클릭 시 최신 계약 상세 조회 후 다이얼로그 오픈
                                            showProposalDialog = true
                                        }
                                    )
                                }
                            }
                            "SYSTEM" -> {
                                // 가운데 시스템 메시지 (isContractMsg=true 포함)
                                com.billage.app.ui.chat.components.cards.SystemNoticeCard(
                                    message = com.billage.app.ui.chat.models.SystemNoticeMessage(
                                        id = msg.id,
                                        timestamp = java.time.LocalDateTime.now(),
                                        text = msg.content
                                    )
                                )
                            }
                            else -> {
                                if (msg.isContractMsg) {
                                    // 기존 isContractMsg true 텍스트도 SystemNoticeCard로
                                    com.billage.app.ui.chat.components.cards.SystemNoticeCard(
                                        message = com.billage.app.ui.chat.models.SystemNoticeMessage(
                                            id = msg.id,
                                            timestamp = java.time.LocalDateTime.now(),
                                            text = msg.content
                                        )
                                    )
                                } else {
                                    BaseChatCard(sender = if (isMe) MessageSender.ME else MessageSender.OTHER) {
                                        Column {
                                            if (!isMe) {
                                                Text(
                                                    text = msg.senderName,
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
                }
            }
        )
    }

    // fallback for route without args just in case
    composable(route = AppDestination.ChatRoom.route) {
        val viewModel: com.billage.app.ui.chat.ChatRoomViewModel = hiltViewModel()
        LaunchedEffect(Unit) { viewModel.joinRoom("100", "테스트방") }
        val messages by viewModel.messages.collectAsState()
        val contracts by viewModel.contracts.collectAsState()

        com.billage.app.ui.chat.ChatRoomScreen(
            state = ChatRoomState(
                roomId = "100",
                roomName = "알 수 없음"
            ),
            onBackClick = { appNavController.popBackStack() },
            contracts = contracts,
            onFetchContracts = { viewModel.fetchContracts() },
            onModeChange = {},
            onAddMenuClick = {},
            onSendMessage = { content -> viewModel.sendMessage(content) },
            onDirectEditClick = { contractId -> appNavController.navigateToContractEdit(contractId) },
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
                                            text = msg.senderName,
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
        val menuProfileViewModel: MenuProfileViewModel = hiltViewModel()
        val menuProfileState by menuProfileViewModel.uiState.collectAsState()
        ContractListScreen(
            onNavigateHome = { appNavController.navigateToHome() },
            onNotificationClick = { /* TODO: 알림 화면으로 이동 */ },
            onAddContractClick = { appNavController.navigateToContract() },
            onChatClick = { appNavController.navigateToChatList() },
            onSettingsClick = { appNavController.navigateToSettings() },
            onLogoutClick = { appNavController.navigateToLogin(clearBackStack = true) },
            onContractDetailClick = { contractId -> appNavController.navigateToContractDetail(contractId.toString()) },
            menuProfileState = menuProfileState
        )
    }

    // ← 특약 추가 화면 (ContractComplete 바로 위에 위치)
    composable(route = AppDestination.Settings.route) {
        WithdrawalSettingsScreen(
            onBackClick = { appNavController.popBackStack() },
            onWithdrawSuccess = { appNavController.navigateToLogin(clearBackStack = true) }
        )
    }

    composable(route = AppDestination.ContractSpecialTerms.routeWithArg) { backStackEntry ->
        val contractSeq = backStackEntry.arguments?.getString("contractSeq")
            ?.toIntOrNull() ?: -1

        ContractSpecialTermsScreen(
            onSkip = { appNavController.navigateToContractComplete() },
            onComplete = { appNavController.navigateToContractComplete() },
            viewModel = hiltViewModel()  // ← hiltViewModel()로 교체
        )
    }

    composable(route = AppDestination.ContractComplete.route) { entry ->
        val parentEntry = remember(entry) {
            try {
                appNavController.navController
                    .getBackStackEntry(AppDestination.Contract.routeWithArg)
            } catch (e: IllegalArgumentException) {
                entry
            }
        }
        val viewModel: ContractViewModel = viewModel(parentEntry)
        val formState by viewModel.formState.collectAsState()

        ContractCompleteScreen(
            formState = formState,
            onHomeClick = { appNavController.popToHome() },
            onChatClick = { appNavController.navigateToChatListFromContract() },
            role = formState.role
        )
    }

    composable(route = AppDestination.ContractEdit.routeWithArg) { backStackEntry ->
        val contractId = backStackEntry.arguments?.getString("contractId") ?: ""
        val viewModel: com.billage.app.ui.contract.ContractDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel()

        androidx.compose.runtime.LaunchedEffect(contractId) {
            contractId.toIntOrNull()?.let { viewModel.fetchContractDetail(it) }
        }

        com.billage.app.ui.contract.ContractEditScreen(
            contractSeq = contractId,
            viewModel = viewModel,
            onBackClick = { appNavController.popBackStack() },
            onSaveSuccess = { appNavController.popBackStack() }
        )
    }

    composable(route = AppDestination.ContractDetail.routeWithArg) { backStackEntry ->
        val contractId = backStackEntry.arguments?.getString("contractId") ?: ""
        val viewModel: com.billage.app.ui.contract.ContractDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel()

        androidx.compose.runtime.LaunchedEffect(contractId) {
            contractId.toIntOrNull()?.let { viewModel.fetchContractDetail(it) }
        }

        com.billage.app.ui.contract.ContractDetailScreen(
            contractSeq = contractId,
            viewModel = viewModel,
            onBackClick = { appNavController.popBackStack() }
        )
    }
}

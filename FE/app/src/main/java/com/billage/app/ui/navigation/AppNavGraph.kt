package com.billage.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.billage.app.ui.auth.ConditionsOfUseScreen
import com.billage.app.ui.auth.LoginScreen
import com.billage.app.ui.contract.list.ContractListScreen
import com.billage.app.ui.contract.create.ContractCreateScreen
import com.billage.app.ui.home.HomeScreen
import com.billage.app.ui.common.MenuProfileViewModel
import com.billage.app.ui.chat.components.ChatTopBar
import com.billage.app.ui.chat.components.ContractSignatureDialog
import com.billage.app.ui.chat.components.PinnedContractBanner
import com.billage.app.ui.chat.components.SignedContractFullScreen
import com.billage.app.ui.chat.components.ContractDocumentFullScreen
import com.billage.app.ui.chatbot.ChatBotScreen
import com.billage.app.ui.chat.models.ChatRoomState
import com.billage.app.ui.chat.models.MessageSender
import com.billage.app.ui.chat.models.AiProposalPayload
import com.billage.app.ui.chat.models.AiProposalMessage
import kotlinx.serialization.decodeFromString
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.billage.app.ui.contract.create.ContractCreateCompleteScreen
import com.billage.app.ui.contract.create.ContractRole
import com.billage.app.ui.contract.create.ContractSpecialTermsScreen
import com.billage.app.ui.contract.create.ContractCreateViewModel
import com.billage.app.ui.contract.list.ContractRoleFilter
import com.billage.app.ui.contract.list.ContractStatusFilter
import com.billage.app.ui.contract.detail.ContractDetailViewModel
import com.billage.app.ui.contract.detail.PdfDownloadState
import com.billage.app.ui.home.AccountDetailScreen
import com.billage.app.ui.home.AccountScreen
import com.billage.app.ui.home.InsightDetailScreen
import com.billage.app.ui.home.InsightListScreen
import com.billage.app.ui.notification.NotificationScreen
import com.billage.app.ui.settings.WithdrawalSettingsScreen
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

fun NavGraphBuilder.appNavGraph(
    appNavController: AppNavController,
    isHomeScreenProtectionEnabled: Boolean = true,
    onHomeScreenProtectionChanged: (Boolean) -> Unit = {}
) {
    composable(route = AppDestination.Login.route) {
        LoginScreen(
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
            onContractClick = { contractType ->
                when (contractType) {
                    "lent" -> appNavController.navigateToMypage(
                        role = ContractRoleFilter.Lend,
                        status = ContractStatusFilter.Repaying
                    )
                    "borrowed" -> appNavController.navigateToMypage(
                        role = ContractRoleFilter.Borrow,
                        status = ContractStatusFilter.Repaying
                    )
                    else -> appNavController.navigateToMypage()
                }
            },
            onAccountClick = { appNavController.navigateToAccount() },
            onNotificationClick = { appNavController.navigateToNotifications() },
            onAddContractClick = { role -> appNavController.navigateToContract(role) },
            onActivityClick = { /* TODO: 전체 내역 화면으로 이동 */ },
            onChatClick = { appNavController.navigateToChatList() },
            onMypageClick = { appNavController.navigateToMypage() },
            onSettingsClick = { appNavController.navigateToSettings() },
            onLogoutClick = { appNavController.navigateToLogin(clearBackStack = true) },
            onInsightListClick = { appNavController.navigateToInsightList() },
            onInsightDetailClick = { index -> appNavController.navigateToInsightDetail(index) },
            menuProfileState = menuProfileState
        )
    }

    composable(route = AppDestination.Contract.routeWithArg) { backStackEntry ->
        val role = backStackEntry.arguments?.getString("role")
            ?.let { ContractRole.valueOf(it) } ?: ContractRole.CREDITOR
        val initialName = backStackEntry.arguments?.getString("initialName")
        
        ContractCreateScreen(
            onNavigateBack = { appNavController.popBackStack() },
            onContractComplete = { contractSeq, roomId, opponentName ->
                appNavController.navigateToSpecialTerms(contractSeq, roomId, opponentName)
            },
            role = role,
            initialName = initialName
        )
    }

    composable(route = AppDestination.ChatList.route) {
        val menuProfileViewModel: MenuProfileViewModel = hiltViewModel()
        val menuProfileState by menuProfileViewModel.uiState.collectAsState()
        com.billage.app.ui.chat.ChatListScreen(
            onChatRoomClick = { roomId, opponentName -> appNavController.navigateToChatRoom(roomId, opponentName) },
            onChatBotClick = { appNavController.navigateToChatBot() },
            onHomeClick = { appNavController.navigateToHome() },
            onAddContractClick = { role -> appNavController.navigateToContract(role) },
            onContractClick = { appNavController.navigateToMypage() },
            onAccountClick = { appNavController.navigateToAccount() },
            onInsightListClick = { appNavController.navigateToInsightList() },
            onSettingsClick = { appNavController.navigateToSettings() },
            onLogoutClick = { appNavController.navigateToLogin(clearBackStack = true) },
            menuProfileState = menuProfileState
        )
    }

    composable(route = AppDestination.ChatBot.route) {
        ChatBotScreen(
            viewModel = hiltViewModel(),
            onBackClick = { appNavController.popBackStack() }
        )
    }


    // fallback for route without args just in case
    composable(route = AppDestination.ChatRoom.routeWithArg) { backStackEntry ->
        val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
        val opponentName = backStackEntry.arguments?.getString("opponentName") ?: ""
        val isNewContract = backStackEntry.arguments?.getString("isNewContract")?.toBoolean() ?: false
        val contractSeq = backStackEntry.arguments?.getString("contractSeq")?.toIntOrNull()

        val viewModel: com.billage.app.ui.chat.ChatRoomViewModel = hiltViewModel()
        LaunchedEffect(roomId, contractSeq) { 
            viewModel.joinRoom(roomId, opponentName, isNewContract, contractSeq) 
        }
        
        val messages by viewModel.messages.collectAsState()
        val contracts by viewModel.contracts.collectAsState()
        val chatRoomState by viewModel.chatRoomState.collectAsState()

        var showProposalDialog by remember { mutableStateOf(false) }
        var showSignedContractDialog by remember { mutableStateOf(false) }
        var currentOriginalPrincipal by remember { mutableStateOf<Double?>(null) }
        var currentOriginalInterest by remember { mutableStateOf<Double?>(null) }
        var currentProposedPrincipal by remember { mutableStateOf<Double?>(null) }
        var currentProposedInterest by remember { mutableStateOf<Double?>(null) }
        var currentProposedModifications by remember { mutableStateOf<List<String>?>(null) }
        var isInitialDialog by remember { mutableStateOf(false) }

        // 차용증 전문/제안서 다이얼로그
        if (showProposalDialog && chatRoomState.draftContractDetail != null) {
            ContractDocumentFullScreen(
                contractTitle = chatRoomState.draftContractDetail?.contractId ?: "ID없음",
                contractDetail = chatRoomState.draftContractDetail!!,
                isMeAccepted = chatRoomState.isMeAccepted,
                isOpponentAccepted = chatRoomState.isOpponentAccepted,
                originalPrincipal = currentOriginalPrincipal,
                originalInterestRate = currentOriginalInterest,
                proposedPrincipal = currentProposedPrincipal,
                proposedInterestRate = currentProposedInterest,
                proposedModifications = currentProposedModifications,
                isInitial = isInitialDialog,
                onDismissRequest = { showProposalDialog = false },
                onAccept = {
                    showProposalDialog = false
                    viewModel.submitProposal()
                },
                onEdit = {
                    showProposalDialog = false
                    chatRoomState.draftContractDetail?.contractSeq?.let { seq ->
                        appNavController.navigateToContractEdit(seq, roomId)
                    }
                },
                contractAiAnalysis = chatRoomState.aiAnalysisResult,
                rawAiAnalysisJson = chatRoomState.rawAiAnalysisJson,
                isAiAnalyzing = chatRoomState.isAiAnalyzing
            )
        }

        // 체결 완료된 차용증 전문 보기 다이얼로그 (Read-only)
        if (showSignedContractDialog && chatRoomState.draftContractDetail != null) {
            SignedContractFullScreen(
                contractTitle = chatRoomState.draftContractDetail?.contractId ?: "ID없음",
                contractDetail = chatRoomState.draftContractDetail!!,
                onDismissRequest = { showSignedContractDialog = false }
            )
        }

        com.billage.app.ui.chat.ChatRoomScreen(
            state = chatRoomState,
            contracts = contracts,
            onFetchContracts = { viewModel.fetchContracts() },
            onBackClick = { appNavController.popBackStack() },
            onModeChange = { mode -> viewModel.setMode(mode) },
            onSendMessage = { content -> viewModel.sendMessage(content) },
            onAddMenuClick = {},
            onRequestAiProposal = { viewModel.requestAiProposal() },
            onContractSelectForModify = { contract -> viewModel.setModificationMode(contract) },
            onAiNegotiateSelect = { contract -> 
                viewModel.startAiNegotiation(
                    com.billage.app.ui.chat.models.ContractSummary(
                        contractSeq = contract.contractSeq,
                        contractId = contract.contractId ?: "ID없음",
                        counterpartyName = contract.counterpartyName ?: "미상"
                    )
                )
            },
            onAnalyzeNegotiation = { viewModel.analyzeCurrentNegotiation() },
            onToggleClauseSelection = { index -> viewModel.toggleClauseSelection(index) },
            onApplyAiClauses = { viewModel.applySelectedAiClauses() },
            onExitModificationMode = { viewModel.exitModificationMode() },
            onCancelProposal = { viewModel.cancelProposal() },
            onSubmitSignature = { request -> viewModel.submitContractSignature(request) },
            onDismissSignatureFeedback = { viewModel.clearSignatureFeedback() },
            onDirectEditClick = { seq -> appNavController.navigateToContractEdit(seq, roomId) },
            messageListContent = {

                val lastAiProposalIds = remember(messages) {
                    val map = mutableMapOf<Int, String>()
                    messages.filter { it.msgType == "AI_PROPOSAL" }.forEach { msg ->
                        try {
                            val payload = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                                .decodeFromString<com.billage.app.ui.chat.models.AiProposalPayload>(msg.content)
                            map[payload.contractSeq] = msg.id
                        } catch (e: Exception) {}
                    }
                    map
                }

                LazyColumn(
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages.asReversed()) { msg ->
                        val isMe = msg.senderSeq == viewModel.myUserSeq
                        val chatRoomState by viewModel.chatRoomState.collectAsState()
                        val contractStatus = chatRoomState.draftContractDetail?.status
                        val isContractReadOnly = contractStatus != null && contractStatus != "DRAFT"
                        if (msg.msgType == "SYSTEM") {
                            com.billage.app.ui.chat.components.cards.SystemNoticeCard(
                                message = com.billage.app.ui.chat.models.SystemNoticeMessage(
                                    id = msg.id,
                                    timestamp = java.time.LocalDateTime.now(),
                                    text = msg.content
                                )
                            )
                        } else if (msg.msgType == "AI_PROPOSAL") {
                             // AI 카드 (차용증 초안 포함)
                             val payload = try {
                                 kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                                     .decodeFromString<com.billage.app.ui.chat.models.AiProposalPayload>(msg.content)
                             } catch (e: Exception) { null }

                             if (payload != null) {
                                 com.billage.app.ui.chat.components.cards.AiProposalCard(
                                     message = com.billage.app.ui.chat.models.AiProposalMessage(
                                         id = msg.id,
                                         sender = if (isMe) MessageSender.ME else MessageSender.OTHER,
                                         timestamp = java.time.LocalDateTime.now(),
                                         contractTitle = payload.contractTitle,
                                         originalPrincipal = payload.originalPrincipal,
                                         proposedPrincipal = payload.proposedPrincipal,
                                         originalInterestRate = payload.originalInterestRate ?: 0.0,
                                         proposedInterestRate = payload.proposedInterestRate ?: (payload.originalInterestRate ?: 0.0),
                                         senderRole = payload.senderRole,
                                         senderName = payload.senderName,
                                         modifications = payload.modifications,
                                         isInitial = payload.isInitial
                                     ),
                                      onViewDetailsClick = {
                                          currentOriginalPrincipal = payload.originalPrincipal
                                          currentOriginalInterest = payload.originalInterestRate
                                          currentProposedPrincipal = payload.proposedPrincipal
                                          currentProposedInterest = payload.proposedInterestRate
                                          currentProposedModifications = payload.modifications
                                          isInitialDialog = payload.isInitial
                                          viewModel.fetchContractDetail(payload.contractSeq)
                                          showProposalDialog = true
                                      },
                                     isEnabled = !isContractReadOnly && lastAiProposalIds[payload.contractSeq] == msg.id
                                 )
                             }
                        } else if (msg.msgType == "CONTRACT_COMPLETE") {
                             // 체결 완료 카드
                             val payload = try {
                                 kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                                     .decodeFromString<com.billage.app.ui.chat.models.ContractCompletePayload>(msg.content)
                             } catch (e: Exception) { null }

                             if (payload != null) {
                                 com.billage.app.ui.chat.components.cards.ContractCompleteCard(
                                     message = com.billage.app.ui.chat.models.ContractCompleteMessage(
                                         id = msg.id,
                                         sender = com.billage.app.ui.chat.models.MessageSender.SYSTEM,
                                         timestamp = java.time.LocalDateTime.now(),
                                         contractId = payload.contractId,
                                         contractTitle = payload.contractTitle,
                                         principalAmount = payload.principalAmount,
                                         interestRate = payload.interestRate
                                     ),
                                     onViewDetailsClick = {
                                         viewModel.fetchContractDetail(payload.contractSeq)
                                         showSignedContractDialog = true
                                     }
                                 )
                             }
                        } else {
                            // 일반 메시지
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
        )
    }

    composable(
        route = AppDestination.Mypage.routeWithArgs,
        arguments = listOf(
            androidx.navigation.navArgument("role") {
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null
            },
            androidx.navigation.navArgument("status") {
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val initialRole = backStackEntry.arguments?.getString("role")
            ?.let { runCatching { ContractRoleFilter.valueOf(it) }.getOrNull() }
        val initialStatus = backStackEntry.arguments?.getString("status")
            ?.let { runCatching { ContractStatusFilter.valueOf(it) }.getOrNull() }
        val menuProfileViewModel: MenuProfileViewModel = hiltViewModel()
        val menuProfileState by menuProfileViewModel.uiState.collectAsState()
        ContractListScreen(
            onNavigateHome = { appNavController.navigateToHome() },
            onNotificationClick = { appNavController.navigateToNotifications() },
            onAddContractClick = { role -> appNavController.navigateToContract(role) },
            onChatClick = { appNavController.navigateToChatList() },
            onAccountClick = { appNavController.navigateToAccount() },
            onInsightListClick = { appNavController.navigateToInsightList() },
            onSettingsClick = { appNavController.navigateToSettings() },
            onLogoutClick = { appNavController.navigateToLogin(clearBackStack = true) },
            onContractDetailClick = { contractId -> appNavController.navigateToContractDetail(contractId.toString()) },
            initialRole = initialRole,
            initialStatus = initialStatus,
            menuProfileState = menuProfileState
        )
    }

    // ← 특약 추가 화면 (ContractComplete 바로 위에 위치)
    composable(route = AppDestination.Settings.route) {
        WithdrawalSettingsScreen(
            onBackClick = { appNavController.popBackStack() },
            onWithdrawSuccess = { appNavController.navigateToLogin(clearBackStack = true) },
            isHomeScreenProtectionEnabled = isHomeScreenProtectionEnabled,
            onHomeScreenProtectionChanged = onHomeScreenProtectionChanged
        )
    }

    composable(route = AppDestination.Notification.route) {
        NotificationScreen(
            onBackClick = { appNavController.popBackStack() },
            onNotificationClick = { notification ->
                notification.contractSeq?.let { contractSeq ->
                    appNavController.navigateToContractDetail(contractSeq.toString())
                }
            }
        )
    }

    composable(route = AppDestination.ContractSpecialTerms.routeWithArg) { backStackEntry ->
        val contractSeq = backStackEntry.arguments?.getString("contractSeq")
            ?.toIntOrNull() ?: -1
        val roomId = backStackEntry.arguments?.getString("roomId") ?: "none"
        val opponentName = backStackEntry.arguments?.getString("opponentName") ?: "알 수 없음"

        ContractSpecialTermsScreen(
            onSkip = { appNavController.navigateToContractComplete(contractSeq, roomId, opponentName) },
            onComplete = { appNavController.navigateToContractComplete(contractSeq, roomId, opponentName) },
            viewModel = hiltViewModel()
        )
    }

    composable(route = AppDestination.ContractComplete.routeWithArg) { backStackEntry ->
        val contractSeq = backStackEntry.arguments?.getString("contractSeq") ?: ""
        val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
        val opponentName = backStackEntry.arguments?.getString("opponentName") ?: ""
        val context = LocalContext.current

        val parentEntry = remember(backStackEntry) {
            try {
                appNavController.navController
                    .getBackStackEntry(AppDestination.Contract.routeWithArg)
            } catch (e: IllegalArgumentException) {
                backStackEntry
            }
        }
        val viewModel: ContractCreateViewModel = viewModel(parentEntry)
        val formState by viewModel.formState.collectAsState()

        val pdfViewModel: ContractDetailViewModel = hiltViewModel()
        val pdfDownloadState by pdfViewModel.pdfDownloadState.collectAsState()

        LaunchedEffect(pdfDownloadState) {
            when (pdfDownloadState) {
                is PdfDownloadState.Success -> {
                    Toast.makeText(context, "Downloads 폴더에 저장되었어요", Toast.LENGTH_SHORT).show()
                    pdfViewModel.clearPdfDownloadState()
                }
                is PdfDownloadState.Error -> {
                    Toast.makeText(context, "다운로드에 실패했어요. 다시 시도해주세요", Toast.LENGTH_SHORT).show()
                    pdfViewModel.clearPdfDownloadState()
                }
                else -> {}
            }
        }

        ContractCreateCompleteScreen(
            formState = formState,
            onHomeClick = { appNavController.popToHome() },
            onChatClick = {
                if (roomId.isNotEmpty()) {
                    appNavController.navigateToChatRoom(
                        roomId = roomId,
                        opponentName = opponentName,
                        isNewContract = true,
                        contractSeq = contractSeq.toIntOrNull() ?: -1,
                        clearContractFlow = true
                    )
                } else {
                    appNavController.navigateToChatListFromContract()
                }
            },
            onDownloadPdfClick = {
                contractSeq.toIntOrNull()?.let { pdfViewModel.downloadPdf(it) }
            },
            role = formState.role
        )
    }

    composable(
        route = AppDestination.ContractEdit.routeWithArg,
        arguments = listOf(
            androidx.navigation.navArgument("contractSeq") { type = androidx.navigation.NavType.StringType },
            androidx.navigation.navArgument("roomId") {
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val contractSeq = backStackEntry.arguments?.getString("contractSeq")?.toIntOrNull() ?: -1
        val roomId = backStackEntry.arguments?.getString("roomId")
        val viewModel: com.billage.app.ui.contract.detail.ContractDetailViewModel = hiltViewModel(
            checkNotNull<androidx.lifecycle.ViewModelStoreOwner>(LocalViewModelStoreOwner.current) {
                    "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
                },
            null
        )

        androidx.compose.runtime.LaunchedEffect(contractSeq) {
            if (contractSeq != -1) viewModel.fetchContractDetail(contractSeq)
        }

        com.billage.app.ui.contract.detail.ContractEditScreen(
            contractSeq = contractSeq.toString(),
            viewModel = viewModel,
            roomId = roomId,
            onBackClick = { appNavController.popBackStack() },
            onSaveSuccess = { appNavController.popBackStack() }
        )
    }

    composable(route = AppDestination.InsightList.route) {
        InsightListScreen(
            onInsightClick = { index -> appNavController.navigateToInsightDetail(index) },
            onBackClick = { appNavController.popBackStack() }
        )
    }

    composable(route = AppDestination.InsightDetail.routeWithArg) { backStackEntry ->
        val insightIndex = backStackEntry.arguments?.getString("insightIndex")?.toIntOrNull() ?: 0
        InsightDetailScreen(
            insightIndex = insightIndex,
            onBackClick = { appNavController.popBackStack() }
        )
    }

    composable(route = AppDestination.Account.route) {
        AccountScreen(
            onBackClick = { appNavController.popBackStack() },
            onTransactionClick = { transactionUniqueNo -> appNavController.navigateToAccountDetail(transactionUniqueNo) }
        )
    }

    composable(route = AppDestination.AccountDetail.routeWithArg) { backStackEntry ->
        val transactionUniqueNo = backStackEntry.arguments?.getString("transactionUniqueNo")?.toLongOrNull() ?: -1L
        AccountDetailScreen(
            transactionUniqueNo = transactionUniqueNo,
            onBackClick = { appNavController.popBackStack() }
        )
    }

    composable(route = AppDestination.ContractDetail.routeWithArg) { backStackEntry ->
        val contractId = backStackEntry.arguments?.getString("contractId") ?: ""
        val viewModel: com.billage.app.ui.contract.detail.ContractDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel()

        androidx.compose.runtime.LaunchedEffect(contractId) {
            contractId.toIntOrNull()?.let {
                viewModel.fetchContractDetail(it)
                viewModel.fetchRepaymentSchedules(it)
            }
        }

        com.billage.app.ui.contract.detail.ContractDetailScreen(
            contractSeq = contractId,
            viewModel = viewModel,
            onBackClick = { appNavController.popBackStack() },
            onViewDocumentClick = {
                contractId.toIntOrNull()?.let { appNavController.navigateToContractDocument(it) }
            },
            onNavigateToChat = { roomId, opponentName ->
                appNavController.navigateToChatRoom(roomId, opponentName)
            }
        )
    }

    composable(route = AppDestination.ContractDocument.routeWithArg) { backStackEntry ->
        val contractSeq = backStackEntry.arguments?.getString("contractSeq")?.toIntOrNull() ?: -1
        val viewModel: com.billage.app.ui.contract.detail.ContractDetailViewModel =
            androidx.hilt.navigation.compose.hiltViewModel()

        androidx.compose.runtime.LaunchedEffect(contractSeq) {
            if (contractSeq != -1) {
                viewModel.fetchContractDetail(contractSeq)
                viewModel.fetchContractHtml(contractSeq)
            }
        }

        com.billage.app.ui.contract.detail.ContractDocumentScreen(
            viewModel = viewModel,
            onBackClick = { appNavController.popBackStack() }
        )
    }
}

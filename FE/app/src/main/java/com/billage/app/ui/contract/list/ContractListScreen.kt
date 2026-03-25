package com.billage.app.ui.contract.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.billage.app.ui.common.BottomBar
import com.billage.app.ui.common.BottomBarTab
import com.billage.app.ui.common.MenuProfileState
import com.billage.app.ui.common.TopBar
import com.billage.app.ui.common.rememberLogoutUiState
import com.billage.app.ui.home.HomeSideMenu
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

/**
 * 내 계약 전체보기 화면.
 * - 역할(빌려줌/빌림)과 상태(서명중/상환중/완료)로 계약 필터링.
 * - 각 카드 클릭 시 [onContractDetailClick]에 contractSeq 전달.
 */
@Composable
fun ContractListScreen(
    onNavigateHome: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onAddContractClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onInsightListClick: () -> Unit = {},
    onContractDetailClick: (contractId: Int) -> Unit = {},
    menuProfileState: MenuProfileState = MenuProfileState(),
    viewModel: ContractListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.refreshUnreadCount()
    }
    val logoutUiState = rememberLogoutUiState(onLoggedOut = onLogoutClick)
    var showMenu by remember { mutableStateOf(false) }
    val dismissInteraction = remember { MutableInteractionSource() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.deleteError) {
        uiState.deleteError?.let { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
            viewModel.clearDeleteError()
        }
    }

    var selectedRole by rememberSaveable { mutableStateOf(ContractRoleFilter.All) }
    var selectedStatus by rememberSaveable { mutableStateOf(ContractStatusFilter.All) }

    // 필터 변경 시 API 재호출
    // status는 항상 null(전체 조회) — 상태별 apiValues가 복수이므로 서버 필터 불가.
    // 상태 구분은 아래 클라이언트 사이드 필터링에서만 처리.
    LaunchedEffect(selectedRole, selectedStatus) {
        viewModel.fetchContracts(
            role = selectedRole.apiValue,
            status = null
        )
    }

    // 현재 선택된 필터에 맞춰 목록 클라이언트 사이드 필터링 (옵션)
    val filteredContracts = remember(uiState.contracts, selectedRole, selectedStatus) {
        uiState.contracts.filter { contract ->
            (selectedRole == ContractRoleFilter.All || contract.role == selectedRole) &&
            (selectedStatus == ContractStatusFilter.All || contract.status == selectedStatus)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BillageBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopBar(
                    userName = "빌리지",
                    hasNotification = uiState.hasNotification,
                    onNotificationClick = onNotificationClick,
                    onMenuClick = { showMenu = true },
                    showBrandLogo = true
                )
            },
            bottomBar = {
                BottomBar(
                    selectedTab = if (showMenu) BottomBarTab.Menu else BottomBarTab.Contract,
                    onAddContractClick = onAddContractClick,
                    onHomeClick = onNavigateHome,
                    onChatClick = onChatClick,
                    onContractClick = {},
                    onMenuClick = { showMenu = true }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── 헤더 (제목 + 필터) ─────────────────────────────
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = "내 계약 전체보기",
                            color = BillageTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        RoleFilterTabs(
                            selectedRole = selectedRole,
                            onRoleSelected = { selectedRole = it }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        StatusFilterChips(
                            selectedStatus = selectedStatus,
                            onStatusSelected = { selectedStatus = it }
                        )
                    }
                }

                // ── 로딩 / 에러 ────────────────────────────────────
                when {
                    uiState.isLoading -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = BillagePrimary) }
                    }
                    uiState.error != null -> item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "⚠️", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.error ?: "",
                                color = BillageTextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }
                    filteredContracts.isEmpty() -> item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🗂️", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "해당 조건의 계약이 없습니다.",
                                color = BillageTextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> items(filteredContracts, key = { it.id }) { contract ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ContractListCard(
                                contract = contract,
                                onDetailClick = { onContractDetailClick(contract.id) },
                                onDeleteClick = if (contract.status == ContractStatusFilter.Signing) {
                                    { viewModel.deleteContract(contract.id) }
                                } else null
                            )
                        }
                    }
                }

                // ── FAB 여백 ──────────────────────────────────────
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // ── 사이드 메뉴 오버레이 ───────────────────────────────────
        AnimatedVisibility(visible = showMenu, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f))
                    .clickable(interactionSource = dismissInteraction, indication = null) {
                        showMenu = false
                    }
            )
        }

        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                HomeSideMenu(
                    userName = menuProfileState.realName.ifBlank { "빌리지" },
                    email = menuProfileState.email,
                    profileImageUrl = menuProfileState.profileImageUrl,
                    isLoggingOut = logoutUiState.isLoggingOut,
                    onClose = { showMenu = false },
                    onProfileClick = { showMenu = false },
                    onAccountClick = { showMenu = false },
                    onInsightListClick = {
                        showMenu = false
                        onInsightListClick()
                    },
                    onSettingsClick = { showMenu = false; onSettingsClick() },
                    onSupportClick = { showMenu = false },
                    onTermsClick = { showMenu = false },
                    onPrivacyClick = { showMenu = false },
                    onLogoutClick = { showMenu = false; logoutUiState.onLogoutClick() }
                )
            }
        }
    }
}

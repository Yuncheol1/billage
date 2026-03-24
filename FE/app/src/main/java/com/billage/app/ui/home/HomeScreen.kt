package com.billage.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.billage.app.ui.common.BottomBar
import com.billage.app.ui.common.BottomBarTab
import com.billage.app.ui.common.MenuProfileState
import com.billage.app.ui.common.TopBar
import com.billage.app.ui.contract.create.ContractRole
import com.billage.app.ui.common.rememberLogoutUiState
import com.billage.app.ui.home.components.*
import com.billage.app.ui.theme.*

@Composable
fun HomeScreen(
    onContractClick: (String) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onAddContractClick: (ContractRole) -> Unit = {},
    onActivityClick: () -> Unit = {},
    onScheduleAllClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onMypageClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    menuProfileState: MenuProfileState = MenuProfileState(),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showContractTypeSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val logoutUiState = rememberLogoutUiState(onLoggedOut = onLogoutClick)
    val dismissInteraction = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BillageBackground,
            topBar = {
                TopBar(
                    userName = uiState.userName,
                    hasNotification = uiState.hasNotification,
                    onNotificationClick = onNotificationClick,
                    onMenuClick = { showMenu = true }
                )
            },
            bottomBar = {
                BottomBar(
                    selectedTab = if (showMenu) BottomBarTab.Menu else BottomBarTab.Home,
                    onChatClick = onChatClick,
                    onHomeClick = {},
                    onAddContractClick = { showContractTypeSheet = true },
                    onContractClick = onMypageClick,
                    onMenuClick = { showMenu = true }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(title = "나의 계약 현황")
                Spacer(modifier = Modifier.height(12.dp))
                ContractDashboardCard(
                    lentCount = uiState.lentCount,
                    borrowedCount = uiState.borrowedCount,
                    onLentClick = { onContractClick("lent") },
                    onBorrowedClick = { onContractClick("borrowed") },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))
                SectionTitle(title = "알아두면 좋아요")
                Spacer(modifier = Modifier.height(12.dp))
                TipSlider()

                Spacer(modifier = Modifier.height(28.dp))
                SectionTitleWithAction(
                    title = "다가오는 일정",
                    actionLabel = "전체보기 >",
                    onActionClick = onScheduleAllClick
                )
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BillageSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                        ScheduleTimeline(schedules = uiState.upcomingSchedules)
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
                    userName = menuProfileState.realName.ifBlank { uiState.userName },
                    email = menuProfileState.email,
                    isLoggingOut = logoutUiState.isLoggingOut,
                    onClose = { showMenu = false },
                    onProfileClick = { showMenu = false },
                    onContractMenuClick = {
                        showMenu = false
                        onMypageClick()
                    },
                    onReportMenuClick = {
                        showMenu = false
                        onMypageClick()
                    },
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

    // 계약 만들기 타입 선택 바텀시트
    if (showContractTypeSheet) {
        ContractTypeBottomSheet(
            onDismiss = { showContractTypeSheet = false },
            onSelect = { role ->
                showContractTypeSheet = false
                onAddContractClick(role)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContractTypeBottomSheet(
    onDismiss: () -> Unit,
    onSelect: (ContractRole) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BillageSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "계약 만들기",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BillageTextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            ContractTypeItem(
                emoji = "🤝",
                title = "돈 빌려주기",
                description = "차용증을 생성하고 상환 일정을 관리해요",
                onClick = { onSelect(ContractRole.CREDITOR) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ContractTypeItem(
                emoji = "🙏",
                title = "돈 빌리기",
                description = "차용 요청을 보내고 계약을 진행해요",
                onClick = { onSelect(ContractRole.DEBTOR) }
            )
        }
    }
}

@Composable
private fun ContractTypeItem(
    emoji: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = BillageBackground,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    color = BillagePrimaryLight,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 26.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = BillageTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = BillageTextSecondary,
                lineHeight = 18.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = BillageTextSecondary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}

@file:OptIn(ExperimentalMaterial3Api::class)

package com.billage.app.ui.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.billage.app.ui.theme.*
import com.billage.app.ui.chat.models.ChatRoomMode
import com.billage.app.ui.chat.models.ContractSummary
import com.billage.app.data.remote.api.ContractListResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    title: String,
    isNegotiationMode: Boolean = false,
    onNegotiationToggle: (Boolean) -> Unit = {},
    onBackClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BillageSurface),
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = BillageTextPrimary
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BillageTextPrimary
            )
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "협의",
                    style = MaterialTheme.typography.labelMedium,
                    color = BillageTextSecondary,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Switch(
                    checked = isNegotiationMode,
                    onCheckedChange = onNegotiationToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BillagePrimary
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    )
}

@Composable
fun PinnedContractBanner(
    selectedContract: ContractSummary?,
    onCloseClick: () -> Unit,
    onGenerateDraftClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectedContract != null,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(),
            color = BillagePrimary,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 아이콘 데코레이션
                Text(text = "✨", style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.width(8.dp))

                // 텍스트 영역
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "계약 #${selectedContract?.contractId} 협의 중",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 생성 및 닫기 버튼
                Button(
                    onClick = onGenerateDraftClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = BillagePrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("종료 및 초안생성", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ChatBottomInputBar(
    mode: ChatRoomMode,
    inputText: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAddClick: () -> Unit // + 버튼 클릭 시 노출되는 메뉴가 모드에 따라 다름을 처리
) {
    Surface(
        color = BillageSurface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "메뉴 열기",
                    // 계약 모드일 때는 + 버튼의 색상을 다르게 줘서 특수 모드임을 암시
                    tint = if (mode == ChatRoomMode.CONTRACT) BillagePrimary else BillageTextSecondary
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                placeholder = {
                    Text(
                        text = if (mode == ChatRoomMode.CONTRACT) "수정 제안이나 의견을 입력하세요 (AI가 정리합니다)" else "메시지를 입력하세요",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BillageDivider,
                    focusedBorderColor = BillagePrimary
                ),
                maxLines = 4
            )

            IconButton(
                onClick = onSendClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(BillagePrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "전송",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ContractSelectBottomSheet(
    contracts: List<ContractSummary>,
    onDismissRequest: () -> Unit,
    onContractSelect: (ContractSummary) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = BillageSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "어떤 차용증에 대해 논의하시겠습니까?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BillageTextPrimary,
                modifier = Modifier.padding(16.dp)
            )
            Divider()
            LazyColumn {
                items(contracts) { contract ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onContractSelect(contract) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "차용증 #${contract.contractId}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = BillageTextPrimary
                            )
                            Text(
                                text = "대상자: ${contract.counterpartyName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BillageTextSecondary
                            )
                        }
                    }
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = BillageDivider.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun AiContractSelectDialog(
    contracts: List<ContractListResponse>,
    onDismissRequest: () -> Unit,
    onContractSelect: (ContractListResponse) -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.6f)
                .clip(RoundedCornerShape(24.dp)),
            color = BillageSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "🤖 AI와 협의할 차용증 선택",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BillageTextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "현재 진행 중인 계약을 선택하여 챗봇 기반 협의를 시작하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BillageTextSecondary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (contracts.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(contracts) { contract ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BillageBackground),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .aspectRatio(0.85f)
                                    .clickable { onContractSelect(contract) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(Color.White, CircleShape)
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📝", fontSize = 24.sp)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "계약 #${contract.contractSeq}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BillageTextPrimary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${contract.counterpartyName ?: "미상"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BillageTextSecondary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "활성 마감: D-7", // 더미 지표
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BillagePrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "현재 선택 가능한 활성 차용증이 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BillageTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BillageDivider)
                ) {
                    Text("취소", color = BillageTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package com.billage.app.ui.chat.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.billage.app.ui.chat.models.ActionStatus
import com.billage.app.ui.chat.models.AiProposalMessage
import com.billage.app.ui.chat.models.ConditionProposalMessage
import com.billage.app.ui.chat.models.ContractCompleteMessage
import com.billage.app.ui.chat.models.DraftRequestMessage
import com.billage.app.ui.chat.models.MessageSender
import com.billage.app.ui.chat.models.ProposalRequestMessage
import com.billage.app.ui.chat.models.SignatureRequestMessage
import com.billage.app.ui.chat.models.SystemNoticeMessage
import androidx.compose.material3.Surface
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageDivider
import com.billage.app.ui.theme.BillageInfo
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import com.billage.app.ui.theme.BillageTheme
import java.time.LocalDateTime

@Composable
fun DraftRequestCard(
    message: DraftRequestMessage,
    onAcceptClick: () -> Unit,
    onProposeClick: () -> Unit,
    onViewDetailsClick: () -> Unit = {},
    isEnabled: Boolean = true
) {

    BaseChatCard(sender = message.sender) {
        val contentColor = if (message.sender == MessageSender.ME) {
            BillageTextPrimary
        } else {
            BillageTextSecondary
        }

        Column(modifier = Modifier.width(260.dp)) {
            // 헤더 영역
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = BillagePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val titleText = if (message.sender == MessageSender.ME) "내가 돈을 빌려줬어요!" else "${message.senderName}님이 돈을 빌려줬어요!"
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // 내용 영역
            Card(
                colors = CardDefaults.cardColors(containerColor = BillageSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "계약명: ${message.contractTitle}", 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.summary, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = BillageTextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 차용증 전문 보기 (네비게이션 액션)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(enabled = isEnabled) { onViewDetailsClick() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "차용증 원본 보기",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BillagePrimary
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "차용증 전문 보기",
                            tint = BillagePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            if (message.sender == MessageSender.OTHER) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // 액션 버튼 영역 (상태에 따른 분기)
                when (message.actionStatus) {
                    ActionStatus.PENDING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onProposeClick,
                                modifier = Modifier.weight(1f),
                                enabled = isEnabled
                            ) {
                                Text("수정 제안", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = onAcceptClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary),
                                enabled = isEnabled
                            ) {
                                Text("동의", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    ActionStatus.COMPLETED -> ActionCompletedView("초안에 동의했습니다.")
                    ActionStatus.REJECTED -> ActionCompletedView("수정을 제안했습니다.")
                }
            }
        }
    }
}

@Composable
fun ConditionProposalCard(
    message: ConditionProposalMessage,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    isEnabled: Boolean = true
) {
    BaseChatCard(sender = message.sender) {
        val contentColor = if (message.sender == MessageSender.ME) {
            BillageTextPrimary
        } else {
            BillageTextSecondary
        }

        Column(modifier = Modifier.width(260.dp)) {
            // 헤더 영역
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = BillageInfo,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "계약 조건 변경 제안",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // 내용 영역 (변경된 특약리스트 하이라이트)
            Card(
                colors = CardDefaults.cardColors(containerColor = BillageDivider.copy(alpha=0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "수정된 항목:", 
                        fontWeight = FontWeight.SemiBold, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = BillagePrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.modifications.ifEmpty { listOf("특약사항 없음") }.forEachIndexed { index, mod ->
                        Text(
                            text = if (message.modifications.isEmpty()) mod else "제${index + 1}조 $mod", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = BillageTextSecondary
                        )
                        if (index < message.modifications.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 액션 영역
            if (message.sender == MessageSender.OTHER) {
                when (message.actionStatus) {
                    ActionStatus.PENDING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRejectClick,
                                modifier = Modifier.weight(1f),
                                enabled = isEnabled
                            ) {
                                Text("거절", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = onAcceptClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BillageInfo),
                                enabled = isEnabled
                            ) {
                                Text("수락하기", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    ActionStatus.COMPLETED -> ActionCompletedView("제안을 수락했습니다.")
                    ActionStatus.REJECTED -> ActionCompletedView("제안을 거절했습니다.")
                }
            }
        }
    }
}

@Composable
fun SignatureRequestCard(
    message: SignatureRequestMessage,
    onSignClick: () -> Unit,
    isEnabled: Boolean = true
) {
    BaseChatCard(sender = message.sender) {
        Column(modifier = Modifier.width(260.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = BillageDanger,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "최종 서명 대기 중",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BillageTextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 진행 상태 표시 (나/상대방)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BillageSurface)
                    .padding(12.dp)
            ) {
                // 내 서명 상태
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("나의 서명", style = MaterialTheme.typography.bodyMedium, color = BillageTextPrimary)
                    if (message.isMySignatureDone) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "완료", tint = BillagePrimary, modifier = Modifier.size(16.dp))
                    } else {
                        Text("대기중", style = MaterialTheme.typography.bodySmall, color = BillageTextPrimary.copy(alpha=0.5f))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 상대방 서명 상태
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("상대방 서명", style = MaterialTheme.typography.bodyMedium, color = BillageTextPrimary)
                    if (message.isOtherSignatureDone) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "완료", tint = BillagePrimary, modifier = Modifier.size(16.dp))
                    } else {
                        Text("대기중", style = MaterialTheme.typography.bodySmall, color = BillageTextPrimary.copy(alpha=0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // '나'의 서명이 안 된 상태면 버튼 노출
            if (!message.isMySignatureDone) {
                Button(
                    onClick = onSignClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BillageDanger),
                    enabled = isEnabled
                ) {
                    Text("전자 서명 진행하기", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                ActionCompletedView("내 서명 완료 (상대방 대기 중)")
            }
        }
    }
}

@Composable
fun ContractCompleteCard(
    message: ContractCompleteMessage,
    onDashboardClick: (String) -> Unit,
    onDownloadPdfClick: (String) -> Unit
) {
    BaseChatCard(sender = message.sender) {
        Column(modifier = Modifier.width(260.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = BillagePrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "계약이 성사되었습니다!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BillageTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "서로가 모두 전자서명을 완료하여 상환 타임라인이 시작되었습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = BillageTextSecondary.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onDashboardClick(message.contractId) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
            ) {
                Text("계약 대시보드로 이동", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            
            OutlinedButton(
                onClick = { onDownloadPdfClick(message.contractId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("계약서 사본(PDF) 저장", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * 버튼 대신 보여줄 공통 완료 뷰
 */
@Composable
private fun ActionCompletedView(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BillageDivider.copy(alpha = 0.4f))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            style = MaterialTheme.typography.bodyMedium, 
            color = BillageTextSecondary.copy(alpha = 0.7f), 
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SystemNoticeCard(message: SystemNoticeMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BillageDivider.copy(alpha = 0.6f)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = BillageTextSecondary
            )
        }
    }
}

@Composable
fun ProposalRequestCard(
    message: ProposalRequestMessage,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    isEnabled: Boolean = true
) {
    BaseChatCard(sender = message.sender) {
        val contentColor = if (message.sender == MessageSender.ME) {
            BillageTextPrimary
        } else {
            BillageTextSecondary
        }

        Column(modifier = Modifier.width(260.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = BillagePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "차용증 수정 제안",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = BillageSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "대상: ${message.contractTitle}", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = BillageTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "상대방이 위 계약건에 대해 조건 수정을 제안했습니다. 수락하시면 협의 모드가 켜집니다.", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = BillageTextPrimary.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (message.sender == MessageSender.OTHER) {
                Spacer(modifier = Modifier.height(12.dp))
                
                when (message.actionStatus) {
                    ActionStatus.PENDING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRejectClick,
                                modifier = Modifier.weight(1f),
                                enabled = isEnabled
                            ) {
                                Text("거절", style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = onAcceptClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary),
                                enabled = isEnabled
                            ) {
                                Text("수락", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    ActionStatus.COMPLETED -> ActionCompletedView("제안을 수락하여 협의를 시작했습니다.")
                    ActionStatus.REJECTED -> ActionCompletedView("제안을 거절했습니다.")
                }
            }
        }
    }
}

@Composable
fun AiProposalCard(
    message: AiProposalMessage,
    onViewDetailsClick: () -> Unit,
    isEnabled: Boolean = true
) {
    BaseChatCard(sender = message.sender) {
        Column(modifier = androidx.compose.ui.Modifier.width(260.dp)) {
            // 헤더
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = androidx.compose.ui.Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (message.isInitial) Icons.Default.Assignment else Icons.Default.Edit,
                    contentDescription = null,
                    tint = BillagePrimary,
                    modifier = androidx.compose.ui.Modifier.size(18.dp)
                )
                Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                val headerText = when {
                    message.isInitial && message.senderRole == "CREDITOR" -> 
                        "😊 ${message.senderName ?: "상대방"}님이 돈을 빌려줬어요"
                    message.isInitial && message.senderRole == "DEBTOR" -> 
                        "😊 ${message.senderName ?: "상대방"}님이 돈을 요청했어요"
                    message.isInitial -> "😊 소중한 거래 제안이 도착했어요."
                    else -> "\uD83E\uDD16 AI 수정 제안 도착"
                }

                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BillageTextPrimary
                )
            }
            
            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            
            // 요약 정보 (미리보기 영역 강화)
            Card(
                colors = CardDefaults.cardColors(containerColor = BillageSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = androidx.compose.ui.Modifier.fillMaxWidth()
            ) {
                Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
                    Text(
                        text = message.contractTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BillageTextPrimary
                    )
                    
                    androidx.compose.material3.HorizontalDivider(
                        modifier = androidx.compose.ui.Modifier.padding(vertical = 12.dp),
                        color = BillageDivider.copy(alpha = 0.5f)
                    )
                    
                    if (message.proposedInterestRate != null) {
                        val isRateChanged = !message.isInitial && message.originalInterestRate != null && 
                                           message.originalInterestRate != message.proposedInterestRate
                        
                        Text(
                            text = when {
                                message.isInitial -> "약정 이자율"
                                isRateChanged -> "이자율 조정됨"
                                else -> "이자율"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = BillageTextSecondary,
                            modifier = androidx.compose.ui.Modifier.padding(bottom = 4.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!message.isInitial && message.originalInterestRate != null && 
                                message.originalInterestRate != message.proposedInterestRate) {
                                Text(
                                    text = "%.2f%%".format(message.originalInterestRate * 100),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BillageTextSecondary,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                                Text(
                                    text = " → ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BillagePrimary
                                )
                            }
                            Text(
                                text = "%.2f%%".format((message.proposedInterestRate ?: 0.0) * 100),
                                style = if (message.isInitial) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                                color = BillagePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (!message.isInitial && message.modifications.isNotEmpty()) {
                        Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
                        val filteredMods = message.modifications.filter { it.isNotBlank() }
                        if (filteredMods.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = androidx.compose.ui.Modifier
                                        .size(4.dp)
                                        .background(BillageTextSecondary, androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                                Text(
                                    text = "특약사항 없음",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BillageTextSecondary
                                )
                            }
                        } else {
                            filteredMods.forEachIndexed { index, mod ->
                                val isAdd = mod.startsWith("ADD: ")
                                val isDel = mod.startsWith("DEL: ")
                                val displayMod = when {
                                    isAdd -> mod.substringAfter("ADD: ")
                                    isDel -> mod.substringAfter("DEL: ")
                                    else -> mod
                                }
                                
                                val contentColor = when {
                                    isAdd -> BillagePrimary
                                    isDel -> BillageDanger
                                    else -> BillageTextSecondary
                                }

                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = androidx.compose.ui.Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isAdd -> "+"
                                            isDel -> "-"
                                            else -> "•"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = contentColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = androidx.compose.ui.Modifier.width(12.dp)
                                    )
                                    Text(
                                        text = displayMod,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = contentColor,
                                        textDecoration = if (isDel) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
            
            // 자세히보기 버튼
            androidx.compose.material3.Button(
                onClick = onViewDetailsClick,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Search,
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.size(18.dp)
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    Text(
                        text = "제안서 자세히 보기",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 전자서명이 완료되어 체결된 계약을 보여주는 전용 카드
 */
@Composable
fun ContractCompleteCard(
    message: com.billage.app.ui.chat.models.ContractCompleteMessage,
    onViewDetailsClick: () -> Unit
) {
    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BillagePrimary.copy(alpha = 0.3f)),
            modifier = androidx.compose.ui.Modifier.width(280.dp)
        ) {
            Column(
                modifier = androidx.compose.ui.Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단 완료 아이콘 + 텍스트
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = BillagePrimary,
                    modifier = androidx.compose.ui.Modifier.size(40.dp)
                )
                
                Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
                
                Text(
                    text = "계약 체결 완료",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BillagePrimary
                )
                
                Text(
                    text = "거래가 안전하게 등록되었습니다.",
                    style = MaterialTheme.typography.labelSmall,
                    color = BillageTextSecondary
                )

                HorizontalDivider(
                    modifier = androidx.compose.ui.Modifier.padding(vertical = 16.dp),
                    color = BillagePrimary.copy(alpha = 0.1f)
                )

                // 요약 정보
                Column(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("대여 원금", style = MaterialTheme.typography.labelMedium, color = BillageTextSecondary)
                        Text(
                            text = "%,.0f원".format(message.principalAmount ?: 0.0),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BillageTextPrimary
                        )
                    }
                    Row(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("연 이자율", style = MaterialTheme.typography.labelMedium, color = BillageTextSecondary)
                        Text(
                            text = "%.2f%%".format((message.interestRate ?: 0.0) * 100),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BillagePrimary
                        )
                    }
                }

                Spacer(modifier = androidx.compose.ui.Modifier.height(20.dp))

                // 조회 버튼
                Button(
                    onClick = onViewDetailsClick,
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
                ) {
                    Text(
                        text = "체결 문서 확인하기",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                Text(
                    text = "ID: ${message.contractId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BillageTextSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}


@Preview(showBackground = true, name = "1. 초안 카드 (작업 대기)")
@Composable
fun PreviewDraftRequest_Pending() {
    BillageTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(BillageBackground)
                .padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DraftRequestCard(
                message = DraftRequestMessage(
                    id = "1",
                    sender = MessageSender.OTHER,
                    timestamp = LocalDateTime.now(),
                    contractTitle = "100만원 소액 대출 건",
                    actionStatus = ActionStatus.PENDING
                ),
                onAcceptClick = {},
                onProposeClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "2. 초안 카드 (작업 완료 - 거절)")
@Composable
fun PreviewDraftRequest_Rejected() {
    BillageTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(BillageBackground)
                .padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DraftRequestCard(
                message = DraftRequestMessage(
                    id = "1",
                    sender = MessageSender.OTHER,
                    timestamp = LocalDateTime.now(),
                    contractTitle = "100만원 소액 대출 건",
                    actionStatus = ActionStatus.REJECTED
                ),
                onAcceptClick = {},
                onProposeClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "3. 초안 카드 (작업 완료 - 수락)")
@Composable
fun PreviewDraftRequest_Accepted() {
    BillageTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(BillageBackground)
                .padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DraftRequestCard(
                message = DraftRequestMessage(
                    id = "1",
                    sender = MessageSender.OTHER,
                    timestamp = LocalDateTime.now(),
                    contractTitle = "100만원 소액 대출 건",
                    actionStatus = ActionStatus.COMPLETED
                ),
                onAcceptClick = {},
                onProposeClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "4. 브릿지 카드 (수정 제안 수락 대기)")
@Composable
fun PreviewProposalRequest_Pending() {
    BillageTheme(dynamicColor = false) {
        Column(modifier = Modifier.background(BillageBackground).padding(16.dp)) {
            ProposalRequestCard(
                message = ProposalRequestMessage(
                    id = "2",
                    sender = MessageSender.OTHER,
                    timestamp = LocalDateTime.now(),
                    contractTitle = "100만원 소액 대출 건",
                    actionStatus = ActionStatus.PENDING
                ),
                onAcceptClick = {},
                onRejectClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "5. 최종 조건 제안 카드")
@Composable
fun PreviewConditionProposal() {
    BillageTheme(dynamicColor = false) {
        Column(modifier = Modifier.background(BillageBackground).padding(16.dp)) {
            ConditionProposalCard(
                message = ConditionProposalMessage(
                    id = "3",
                    sender = MessageSender.SYSTEM,
                    timestamp = LocalDateTime.now(),
                    modifications = listOf("상환 기한을 1개월 연장", "이자율을 3%로 변경"),
                    actionStatus = ActionStatus.COMPLETED
                ),
                onAcceptClick = {},
                onRejectClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "6. 시스템 중앙 알림 카드")
@Composable
fun PreviewSystemNotice() {
    BillageTheme(dynamicColor = false) {
        Column(modifier = Modifier.background(BillageBackground).padding(16.dp)) {
            SystemNoticeCard(
                message = SystemNoticeMessage(
                    id = "notice1",
                    timestamp = LocalDateTime.now(),
                    text = "김철수님이 [100만원 소액 대출 건] 초안 수정을 제안했습니다."
                )
            )
        }
    }
}

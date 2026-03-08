package com.billage.app.ui.chat.components

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.billage.app.ui.chat.models.ActionStatus
import com.billage.app.ui.chat.models.ConditionProposalMessage
import com.billage.app.ui.chat.models.ContractCompleteMessage
import com.billage.app.ui.chat.models.DraftRequestMessage
import com.billage.app.ui.chat.models.MessageSender
import com.billage.app.ui.chat.models.SignatureRequestMessage
import com.billage.app.ui.theme.BillageTheme
import java.time.LocalDateTime

@Composable
fun DraftRequestCard(
    message: DraftRequestMessage,
    onAcceptClick: () -> Unit,
    onProposeClick: () -> Unit
) {
    BaseChatCard(sender = message.sender) {
        val contentColor = if (message.sender == MessageSender.ME) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Column(modifier = Modifier.width(260.dp)) {
            // 헤더 영역
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "차용증 초안 확인 요청",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // 내용 영역
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "계약명: ${message.contractTitle}", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "내용을 확인하고 서명을 진행하거나, 수정이 필요한 경우 조건을 제안해 주세요.", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("수정 제안", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = onAcceptClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("수락하기", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                ActionStatus.COMPLETED -> ActionCompletedView("초안이 수락되었습니다.")
                ActionStatus.REJECTED -> ActionCompletedView("수정을 제안했습니다.")
            }
        }
    }
}

@Composable
fun ConditionProposalCard(
    message: ConditionProposalMessage,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    BaseChatCard(sender = message.sender) {
        val contentColor = if (message.sender == MessageSender.ME) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Column(modifier = Modifier.width(260.dp)) {
            // 헤더 영역
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "수정된 항목:", 
                        fontWeight = FontWeight.SemiBold, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.modifications.forEachIndexed { index, mod ->
                        Text(
                            text = "${index + 1}. $mod", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (index < message.modifications.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 액션 영역
            when (message.actionStatus) {
                ActionStatus.PENDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRejectClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("거절", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = onAcceptClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
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

@Composable
fun SignatureRequestCard(
    message: SignatureRequestMessage,
    onSignClick: () -> Unit
) {
    BaseChatCard(sender = message.sender) {
        Column(modifier = Modifier.width(260.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "최종 서명 대기 중",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 진행 상태 표시 (나/상대방)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                // 내 서명 상태
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("나의 서명", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (message.isMySignatureDone) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "완료", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    } else {
                        Text("대기중", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 상대방 서명 상태
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("상대방 서명", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (message.isOtherSignatureDone) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "완료", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    } else {
                        Text("대기중", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // '나'의 서명이 안 된 상태면 버튼 노출
            if (!message.isMySignatureDone) {
                Button(
                    onClick = onSignClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "계약이 성사되었습니다!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "양측 모두 전자서명을 완료하여 상환 타임라인이 시작되었습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onDashboardClick(message.contractId) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), 
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ==========================================
// 도메인 맞춤 카드 프리뷰
// ==========================================
@Preview(showBackground = true)
@Composable
fun PreviewContractCards() {
    BillageTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
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
            
            ConditionProposalCard(
                message = ConditionProposalMessage(
                    id = "2",
                    sender = MessageSender.ME,
                    timestamp = LocalDateTime.now(),
                    modifications = listOf("상환 기한을 1개월 연장", "이자율을 3%로 변경"),
                    actionStatus = ActionStatus.COMPLETED
                ),
                onAcceptClick = {},
                onRejectClick = {}
            )
            
            SignatureRequestCard(
                message = SignatureRequestMessage(
                    id = "3",
                    sender = MessageSender.SYSTEM,
                    timestamp = LocalDateTime.now(),
                    isMySignatureDone = true,
                    isOtherSignatureDone = false
                ),
                onSignClick = {}
            )
            
            ContractCompleteCard(
                message = ContractCompleteMessage(
                    id = "4",
                    sender = MessageSender.SYSTEM,
                    timestamp = LocalDateTime.now(),
                    contractId = "CONT001",
                    contractTitle = "100만원 소액 대출 건"
                ),
                onDashboardClick = {},
                onDownloadPdfClick = {}
            )
        }
    }
}

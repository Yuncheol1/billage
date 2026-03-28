package com.billage.app.ui.chat.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.billage.app.ui.theme.*
import com.billage.app.ui.contract.detail.formatRepaymentMethodFriendly
import com.billage.app.data.remote.api.ContractDetailResponse

/**
 * 전자서명이 완료된 계약서를 실제 차용증 양식처럼 보여주는 읽기 전용 뷰
 */
@Composable
fun SignedContractFullScreen(
    contractTitle: String,
    contractDetail: ContractDetailResponse,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BillageBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // 상단바 (심플하게)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "체결 완료 문서",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BillageTextPrimary
                    )
                }

                HorizontalDivider(color = BillageDivider)

                // 본문 영역 (종이 차용증 느낌)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. 타이틀
                        Text(
                            text = "차 용 증",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 8.sp,
                            color = BillageTextPrimary,
                            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                        )
                        if (!contractDetail.contractId.isNullOrBlank()) {
                            Text(
                                text = "No. ${contractDetail.contractId}",
                                fontSize = 12.sp,
                                color = BillageTextSecondary,
                                modifier = Modifier.padding(bottom = 32.dp)
                            )
                        }

                        // 2. 계약 개요 요약 (표 형식)
                        ContractSummaryDocTable(contractDetail)

                        Spacer(modifier = Modifier.height(32.dp))

                        // 3. 서문
                        Text(
                            text = "채권자와 채무자는 상기 원금 및 이자율을 바탕으로 다음과 같이 금전소비대차계약을 체결하며, 각 조항을 성실히 이행할 것을 약속한다.",
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = BillageTextPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 4. 상세 조항
                        contractDetail.clauses?.forEachIndexed { index, clause ->
                            SignedClauseItem(index + 1, clause.title ?: "특약사항", clause.content)
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        // 5. 날짜 및 확인 문구
                        Text(
                            text = "본 계약의 성립을 증명하기 위해 전자기록을 생성하고\n양측의 전자서명을 완료하였다.",
                            fontSize = 14.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            color = BillageTextPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = contractDetail.signedDttm?.substringBefore("T") ?: "",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BillageTextPrimary
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // 6. 하단 서명 (Mockup)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SignatureUnit(label = "채권자", name = contractDetail.myRealName ?: "나")
                            SignatureUnit(label = "채무자", name = contractDetail.counterpartyName ?: "미상")
                        }
                    }
                }

                // 하단 닫기 버튼
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BillageSurface)
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BillagePrimary)
                    ) {
                        Text("확인 완료", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ContractSummaryDocTable(detail: ContractDetailResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black.copy(alpha = 0.8f))
    ) {
        DocTableRow("차용 원금", "%,d원".format(detail.principalAmount?.toLong() ?: 0L))
        DocTableRow("이자율", "연 %.2f%%".format((detail.interestRate ?: 0.0) * 100))
        DocTableRow("차용 기간", "${detail.startDt ?: "-"} ~ ${detail.endDt ?: "-"}")
        DocTableRow("상환 방식", formatRepaymentMethodFriendly(detail.repaymentMethod, detail.totalInstallments))
    }
}

@Composable
fun DocTableRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .background(Color(0xFFF9F9F9))
                .border(0.5.dp, Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(0.5.dp, Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BillageTextPrimary)
        }
    }
}

@Composable
fun SignedClauseItem(no: Int, title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "제${no}조 ($title)",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = BillageTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            lineHeight = 24.sp,
            color = BillageTextPrimary
        )
    }
}

@Composable
fun SignatureUnit(label: String, name: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 12.sp, color = BillageTextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BillageTextPrimary
            )
        }
    }
}

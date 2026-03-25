package com.billage.app.ui.contract.create.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.contract.create.ContractRole
import com.billage.app.ui.contract.create.UserSearchState
import com.billage.app.ui.common.ProfileAvatar
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageCardBorder
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillageIconBackground
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageSurface
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import com.billage.app.ui.theme.BillageWarningBackground
import com.billage.app.ui.theme.KakaoYellow
import com.billage.app.ui.theme.KakaoTextBrown

// 숫자만 저장하고 화면에만 하이픈을 표시하는 VisualTransformation
// (예: "01012345678" 저장 → 화면에 "010-1234-5678"으로 보여줌)
private object PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = buildString {
            digits.forEachIndexed { index, c ->
                if (index == 3 || index == 7) append('-')
                append(c)
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 7 -> offset + 1
                else -> offset + 2
            }.coerceAtMost(formatted.length)

            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 8 -> offset - 1
                else -> offset - 2
            }.coerceAtMost(digits.length)
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

private fun isValidPhone(phone: String) = phone.matches(Regex("^010\\d{8}$"))

@Composable
fun Step1CounterpartyScreen(
    realName: String,
    onRealNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    searchState: UserSearchState,
    onSearch: () -> Unit,
    role: ContractRole = ContractRole.CREDITOR
) {
    val isSearchEnabled = realName.isNotBlank() && isValidPhone(phone)
            && searchState !is UserSearchState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp)
    ) {
        // ── 헤더 ──────────────────────────────────
        Text(
            text = "계약 만들기",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = BillagePrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (role == ContractRole.CREDITOR) "누구에게\n빌려주시나요?" else "누구에게\n빌리시나요?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = BillageTextPrimary,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── 이름 입력 ──────────────────────────────
        Text(
            text = "상대방 이름",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = realName,
            onValueChange = onRealNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = BillageBackground, shape = RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp),
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = BillageTextPrimary
            ),
            cursorBrush = SolidColor(BillagePrimary),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (realName.isEmpty()) {
                    Text(text = "홍길동", fontSize = 18.sp, color = BillageTextSecondary)
                }
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── 전화번호 입력 + 검색 버튼 ──────────────
        Text(
            text = "전화번호",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = phone,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(11)
                    onPhoneChange(digits)
                },
                modifier = Modifier
                    .weight(1f)
                    .background(color = BillageBackground, shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = BillageTextPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                cursorBrush = SolidColor(BillagePrimary),
                singleLine = true,
                visualTransformation = PhoneVisualTransformation,
                decorationBox = { innerTextField ->
                    if (phone.isEmpty()) {
                        Text(text = "010-0000-0000", fontSize = 18.sp, color = BillageTextSecondary)
                    }
                    innerTextField()
                }
            )

            // 검색 버튼 — 이름 + 전화번호 둘 다 입력됐을 때만 활성화
            Box(
                modifier = Modifier
                    .background(
                        color = if (isSearchEnabled) BillagePrimary else BillageCardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = isSearchEnabled) { onSearch() }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (searchState is UserSearchState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "검색",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 검색 결과 영역 ─────────────────────────
        when (searchState) {

            // 검색 전 안내
            is UserSearchState.Idle -> {
                Text(
                    text = "이름과 전화번호를 입력하고 검색해 주세요",
                    fontSize = 13.sp,
                    color = BillageTextSecondary
                )
            }

            // 검색 중 (버튼에 이미 로딩 표시)
            is UserSearchState.Loading -> {}

            // 등록된 사용자 발견
            is UserSearchState.Found -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "이 분이 맞나요?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BillageTextPrimary
                    )
                    // 검색된 사용자 카드 — 항상 선택된 상태로 표시
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = BillagePrimaryLight, shape = RoundedCornerShape(16.dp))
                            .border(width = 2.dp, color = BillagePrimary, shape = RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 이름 첫 글자 프로필 아이콘
                        ProfileAvatar(
                            name = searchState.name,
                            imageUrl = searchState.profileImageUrl,
                            modifier = Modifier
                                .size(44.dp)
                                .background(color = BillageIconBackground, shape = CircleShape),
                            textSize = 18.sp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = searchState.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BillageTextPrimary
                            )
                            // 저장된 숫자를 하이픈 형식으로 표시
                            Text(
                                text = searchState.phone.let {
                                    "${it.substring(0, 3)}-${it.substring(3, 7)}-${it.substring(7)}"
                                },
                                fontSize = 13.sp,
                                color = BillageTextSecondary
                            )
                        }
                        Text(text = "✓", fontSize = 18.sp, color = BillagePrimary)
                    }
                }
            }

            // 미등록 사용자 — 초대 유도
            is UserSearchState.NotFound -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = BillageWarningBackground, shape = RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "빌리지에 가입되지 않은 번호예요",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BillageTextPrimary
                    )
                    Text(
                        text = "상대방이 빌리지에 가입해야 계약을 진행할 수 있어요.\n초대 메시지를 보내보세요!",
                        fontSize = 13.sp,
                        color = BillageTextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // 카카오톡 초대 버튼
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = KakaoYellow, shape = RoundedCornerShape(10.dp))
                            .clickable {
                                // TODO: 카카오톡 초대 링크 전송 구현
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "카카오톡으로 초대하기",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KakaoTextBrown
                        )
                    }
                }
            }

            // 에러
            is UserSearchState.Error -> {
                Text(
                    text = searchState.message,
                    fontSize = 13.sp,
                    color = BillageDanger
                )
            }
        }
    }
}

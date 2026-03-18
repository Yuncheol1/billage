package com.billage.app.ui.contract.steps

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.billage.app.ui.contract.ContractRole
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

// 검색 결과 더미 데이터 모델
data class UserSearchResult(
    val userSeq: Int,
    val name: String,
    val phone: String
)

private fun isValidPhone(phone: String): Boolean {
    return phone.matches(Regex("^010\\d{8}$"))
}

// 숫자만 저장하고 화면에만 하이픈을 표시하는 VisualTransformation
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

// TODO: BE API 연동 — 전화번호로 회원 검색
// GET /api/users/search?phone={phone}
// 현재는 더미 데이터로 동작
private fun searchUserByPhone(phone: String): List<UserSearchResult> {
    val dummyUsers = listOf(
        UserSearchResult(1, "홍길동", "01012345678"),
        UserSearchResult(2, "김철수", "01087654321"),
        UserSearchResult(3, "이영희", "01011112222"),
    )
    return dummyUsers.filter { it.phone.contains(phone) }
}

@Composable
fun Step6DebtorScreen(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    selectedUserSeq: Int?,
    onUserSelect: (userSeq: Int, name: String, phone: String) -> Unit,
    role: ContractRole = ContractRole.CREDITOR
) {
    val isPhoneValid = isValidPhone(phone)

    // UI 전용 상태 — 검색 결과 및 검색 완료 여부
    var searchResults by remember { mutableStateOf<List<UserSearchResult>?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp)
    ) {
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

        Text(
            text = "전화번호",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 전화번호 입력 + 검색 버튼 한 행
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
                    // 번호 바뀌면 검색 결과 초기화
                    searchResults = null
                    hasSearched = false
                    onUserSelect(-1, "", digits)
                },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = BillageBackground,
                        shape = RoundedCornerShape(16.dp)
                    )
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
                        Text(
                            text = "010-0000-0000",
                            fontSize = 18.sp,
                            color = BillageTextSecondary
                        )
                    }
                    innerTextField()
                }
            )

            // 검색 버튼
            Box(
                modifier = Modifier
                    .background(
                        color = if (isPhoneValid) BillagePrimary else BillageCardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = isPhoneValid) {
                        searchResults = searchUserByPhone(phone)
                        hasSearched = true
                    }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "검색",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 검색 결과 영역
        when {
            // 검색 전
            !hasSearched -> {
                Text(
                    text = "전화번호를 입력하고 검색해주세요",
                    fontSize = 13.sp,
                    color = BillageTextSecondary
                )
            }

            // 검색 결과 없음 — 미가입 안내
            searchResults.isNullOrEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = BillageWarningBackground,
                            shape = RoundedCornerShape(16.dp)
                        )
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
                        text = "상대방이 빌리지에 가입해야 계약을 진행할 수 있어요. 초대 링크를 보내보세요!",
                        fontSize = 13.sp,
                        color = BillageTextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = BillagePrimary,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                // TODO: 카카오톡/문자로 초대 링크 전송
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "초대 링크 보내기",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // 검색 결과 있음
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "이 분이 맞나요?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BillageTextPrimary
                    )

                    searchResults!!.forEach { user ->
                        val isSelected = selectedUserSeq == user.userSeq
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) BillagePrimaryLight else BillageSurface,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) BillagePrimary else BillageCardBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    onUserSelect(user.userSeq, user.name, user.phone)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 프로필 아이콘
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = BillageIconBackground,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.first().toString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BillagePrimary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BillageTextPrimary
                                )
                                Text(
                                    text = user.phone.let {
                                        "${it.substring(0, 3)}-${it.substring(3, 7)}-${it.substring(7)}"
                                    },
                                    fontSize = 13.sp,
                                    color = BillageTextSecondary
                                )
                            }

                            if (isSelected) {
                                Text(text = "✓", fontSize = 18.sp, color = BillagePrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
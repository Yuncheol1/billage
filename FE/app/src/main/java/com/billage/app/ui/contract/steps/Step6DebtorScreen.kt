package com.billage.app.ui.contract.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillageDanger
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary

private fun isValidPhone(phone: String): Boolean {
    return phone.matches(Regex("^010\\d{8}$"))
}

// 숫자만 저장하고 화면에만 하이픈을 표시하는 VisualTransformation
// VisualTransformation — 실제 저장값은 건드리지 않고 화면 표시만 변환하는 Compose 기능
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

@Composable
fun Step6DebtorScreen(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    val isPhoneValid = isValidPhone(phone)
    val showPhoneError = phone.isNotBlank() && !isPhoneValid

    // TextFieldValue — 텍스트 + 커서 위치 + IME 조합 상태를 함께 관리 (한글 입력 깨짐 방지)
    var nameFieldValue by remember { mutableStateOf(TextFieldValue(name)) }

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
            text = "상대방 정보를\n알려주세요",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = BillageTextPrimary,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "이름",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        BasicTextField(
            value = nameFieldValue,
            onValueChange = { newValue ->
                nameFieldValue = newValue
                onNameChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            cursorBrush = SolidColor(BillagePrimary),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (nameFieldValue.text.isEmpty()) {
                    Text(
                        text = "본명을 입력해주세요",
                        fontSize = 18.sp,
                        color = BillageTextSecondary
                    )
                }
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "전화번호",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        BasicTextField(
            value = phone,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(11)
                onPhoneChange(digits)
            },
            modifier = Modifier
                .fillMaxWidth()
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

        Spacer(modifier = Modifier.height(6.dp))

        // 전화번호 상태 안내 문구 — 항상 공간 유지로 레이아웃 밀림 방지
        when {
            showPhoneError -> Text(
                text = "올바른 전화번호 형식이 아니에요",
                fontSize = 12.sp,
                color = BillageDanger
            )
            isPhoneValid -> Text(
                text = "✓ 이 번호로 계약 초대 링크를 보내드려요",
                fontSize = 12.sp,
                color = BillagePrimary
            )
            else -> Text(
                text = "계약 초대 링크를 보낼 번호를 입력해주세요",
                fontSize = 12.sp,
                color = BillageTextSecondary
            )
        }
    }
}
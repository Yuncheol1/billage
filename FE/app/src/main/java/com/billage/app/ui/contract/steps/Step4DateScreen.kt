package com.billage.app.ui.contract.steps

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billage.app.ui.theme.BillageBackground
import com.billage.app.ui.theme.BillagePrimary
import com.billage.app.ui.theme.BillagePrimaryLight
import com.billage.app.ui.theme.BillageTextPrimary
import com.billage.app.ui.theme.BillageTextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일")

// yyyy-MM-dd 문자열을 "2026년 3월 17일" 형태로 변환
private fun formatDisplay(dateStr: String): String {
    return runCatching {
        LocalDate.parse(dateStr, DATE_FORMATTER).format(DISPLAY_FORMATTER)
    }.getOrDefault("")
}

// LocalDate를 yyyy-MM-dd 문자열로 변환
private fun LocalDate.toDateString(): String = format(DATE_FORMATTER)

// 빠른 만기일 선택 — 시작일 기준으로 N개월/년 더하기
private fun addPeriod(startDateStr: String, months: Long): String {
    return runCatching {
        LocalDate.parse(startDateStr, DATE_FORMATTER)
            .plusMonths(months)
            .toDateString()
    }.getOrDefault("")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step4DateScreen(
    startDate: String,
    endDate: String,
    onStartDateSelect: (String) -> Unit,
    onEndDateSelect: (String) -> Unit
) {
    // 시작일 달력 표시 여부
    var showStartDatePicker by remember { mutableStateOf(false) }
    // 만기일 달력 표시 여부
    var showEndDatePicker by remember { mutableStateOf(false) }

    // 시작일이 비어있으면 오늘 날짜 자동 입력
    LaunchedEffect(Unit) {
        if (startDate.isBlank()) {
            onStartDateSelect(LocalDate.now().toDateString())
        }
    }

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
            text = "언제부터 언제까지\n빌려주시나요?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = BillageTextPrimary,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 시작일 섹션
        Text(
            text = "시작일",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        DateDisplayBox(
            dateText = if (startDate.isNotBlank()) formatDisplay(startDate) else "날짜를 선택해주세요",
            isSelected = startDate.isNotBlank(),
            onClick = { showStartDatePicker = true }
        )

        // 오늘 날짜 자동 입력 안내
        if (startDate == LocalDate.now().toDateString()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "✓", fontSize = 12.sp, color = BillagePrimary)
                Text(
                    text = "오늘 날짜가 자동 입력되었어요",
                    fontSize = 12.sp,
                    color = BillageTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 만기일 섹션
        Text(
            text = "만기일",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BillageTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 빠른 선택 버튼 (시작일이 있을 때만 활성화)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "3개월" to 3L,
                "6개월" to 6L,
                "1년" to 12L,
                "2년" to 24L
            ).forEach { (label, months) ->
                val targetDate = if (startDate.isNotBlank()) addPeriod(startDate, months) else ""
                QuickPeriodButton(
                    modifier = Modifier.weight(1f),
                    label = label,
                    isSelected = endDate == targetDate,
                    enabled = startDate.isNotBlank(),
                    onClick = { if (targetDate.isNotBlank()) onEndDateSelect(targetDate) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        DateDisplayBox(
            dateText = if (endDate.isNotBlank()) formatDisplay(endDate) else "날짜를 선택해주세요",
            isSelected = endDate.isNotBlank(),
            onClick = { if (startDate.isNotBlank()) showEndDatePicker = true }
        )

        // 만기일이 선택됐을 때 기간 표시
        if (startDate.isNotBlank() && endDate.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "✓", fontSize = 12.sp, color = BillagePrimary)
                Text(
                    text = "${formatDisplay(startDate)}부터 ${formatDisplay(endDate)}까지",
                    fontSize = 12.sp,
                    color = BillageTextSecondary
                )
            }
        }
    }

    // 시작일 달력
    if (showStartDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                LocalDate.parse(startDate, DATE_FORMATTER)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            }.getOrDefault(System.currentTimeMillis())
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onStartDateSelect(selected.toDateString())
                        // 시작일이 바뀌면 만기일 초기화
                        if (endDate.isNotBlank()) {
                            val end = LocalDate.parse(endDate, DATE_FORMATTER)
                            if (!selected.isBefore(end)) onEndDateSelect("")
                        }
                    }
                    showStartDatePicker = false
                }) { Text("확인", color = BillagePrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("취소", color = BillageTextSecondary)
                }
            }
        ) { DatePicker(state = state) }
    }

    // 만기일 달력 (시작일 이후만 선택 가능)
    if (showEndDatePicker) {
        val minMillis = runCatching {
            LocalDate.parse(startDate, DATE_FORMATTER)
                .plusDays(1)
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())

        val state = rememberDatePickerState(
            initialSelectedDateMillis = runCatching {
                LocalDate.parse(endDate, DATE_FORMATTER)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            }.getOrDefault(minMillis),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onEndDateSelect(selected.toDateString())
                    }
                    showEndDatePicker = false
                }) { Text("확인", color = BillagePrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("취소", color = BillageTextSecondary)
                }
            }
        ) { DatePicker(state = state) }
    }
}

// 날짜 표시 박스 — 탭하면 달력 열림
@Composable
private fun DateDisplayBox(
    dateText: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) BillagePrimaryLight else BillageBackground,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = dateText,
            fontSize = 18.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) BillageTextPrimary else BillageTextSecondary
        )
    }
}

// 빠른 기간 선택 칩 버튼
@Composable
private fun QuickPeriodButton(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) BillagePrimary else BillagePrimaryLight,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) androidx.compose.ui.graphics.Color.White else BillagePrimary
        )
    }
}
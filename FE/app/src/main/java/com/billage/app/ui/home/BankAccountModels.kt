package com.billage.app.ui.home

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

// BE가 Long 필드를 String으로 내려주는 경우를 함께 처리
private object FlexibleLongSerializer : KSerializer<Long> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
    override fun deserialize(decoder: Decoder): Long {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                return element.longOrNull ?: element.content.toLongOrNull() ?: 0L
            }
        }
        return try { decoder.decodeLong() } catch (_: Exception) { 0L }
    }
}

// ─────────────────────────────────────────
// BE API 응답 DTO
// ─────────────────────────────────────────

@Serializable
data class BankAccountDto(
    val accountNo: String = "",
    val bankCode: String = "",
    val bankName: String = "",
    val userName: String = "",
    val accountName: String = "",
    val accountType: String = "",
    val accountDescription: String = "",
    val accountCreatedDate: String = "",
    val accountExpiryDate: String = "",
    @Serializable(with = FlexibleLongSerializer::class)
    val dailyTransferLimit: Long = 0L,
    @Serializable(with = FlexibleLongSerializer::class)
    val oneTimeTransferLimit: Long = 0L,
    @Serializable(with = FlexibleLongSerializer::class)
    val accountBalance: Long = 0L,
    val lastTransactionDate: String = "",
    val currency: String = "KRW"
)

@Serializable
data class TransactionDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val transactionUniqueNo: Long = 0L,
    val transactionDate: String = "",
    val transactionTime: String = "",
    val transactionType: String = "",
    val transactionTypeName: String = "",
    val transactionAccountNo: String = "",
    @Serializable(with = FlexibleLongSerializer::class)
    val transactionBalance: Long = 0L,
    @Serializable(with = FlexibleLongSerializer::class)
    val transactionAfterBalance: Long = 0L,
    val transactionSummary: String = "",
    val transactionMemo: String = ""
)

@Serializable
data class TransactionListDto(
    val totalCount: String = "0",
    val list: List<TransactionDto> = emptyList()
)

// ─────────────────────────────────────────
// 목데이터 (API 연동 완료 시 제거)
// ─────────────────────────────────────────

val mockTransactions = listOf(
    TransactionDto(
        transactionUniqueNo = 1L,
        transactionDate = "20260324",
        transactionTime = "143200",
        transactionType = "1",
        transactionTypeName = "입금",
        transactionAccountNo = "110-123-*****",
        transactionBalance = 300000L,
        transactionAfterBalance = 1300000L,
        transactionSummary = "김철수 상환",
        transactionMemo = "3월 상환금"
    ),
    TransactionDto(
        transactionUniqueNo = 2L,
        transactionDate = "20260323",
        transactionTime = "091500",
        transactionType = "2",
        transactionTypeName = "출금",
        transactionAccountNo = "220-456-*****",
        transactionBalance = 1000000L,
        transactionAfterBalance = 300000L,
        transactionSummary = "이영희 대출",
        transactionMemo = ""
    ),
    TransactionDto(
        transactionUniqueNo = 3L,
        transactionDate = "20260322",
        transactionTime = "184700",
        transactionType = "1",
        transactionTypeName = "입금",
        transactionAccountNo = "330-789-*****",
        transactionBalance = 500000L,
        transactionAfterBalance = 1000000L,
        transactionSummary = "박민수 상환",
        transactionMemo = ""
    )
)

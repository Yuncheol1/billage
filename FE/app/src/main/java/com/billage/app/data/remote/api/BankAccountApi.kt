package com.billage.app.data.remote.api

import com.billage.app.ui.home.BankAccountDto
import com.billage.app.ui.home.TransactionDto
import com.billage.app.ui.home.TransactionListDto
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BankAccountApi {

    // 계좌 보유 여부 확인 (BE 구포맷: data.hasBankAccount)
    @GET("api/v1/bank/me")
    suspend fun checkBankAccount(): BankMeBaseResponse

    // 계좌 생성
    @POST("api/v1/bank/accounts")
    suspend fun createBankAccount(): BankAccountDetailBaseResponse

    // 계좌 상세 + 잔액 조회
    @GET("api/v1/bank/accounts/detail")
    suspend fun getAccountDetail(): BankAccountDetailBaseResponse

    // 거래 목록 조회
    @GET("api/v1/bank/accounts/transactions")
    suspend fun getTransactions(
        @retrofit2.http.Query("startDate") startDate: String,
        @retrofit2.http.Query("endDate") endDate: String
    ): TransactionListBaseResponse

    // 거래 단건 상세
    @GET("api/v1/bank/accounts/transactions/{transactionUniqueNo}")
    suspend fun getTransactionDetail(
        @Path("transactionUniqueNo") transactionUniqueNo: Long
    ): TransactionDetailBaseResponse
}

@Serializable
data class BankMeBaseResponse(
    val status: Int,
    val message: String,
    val data: BankMeData
)

@Serializable
data class BankMeData(
    val hasBankAccount: Boolean
)

@Serializable
data class BankAccountDetailBaseResponse(
    val status: Int,
    val message: String,
    val data: BankAccountDto
)

@Serializable
data class TransactionListBaseResponse(
    val status: Int,
    val message: String,
    val data: TransactionListDto
)

@Serializable
data class TransactionDetailBaseResponse(
    val status: Int,
    val message: String,
    val data: TransactionDto
)

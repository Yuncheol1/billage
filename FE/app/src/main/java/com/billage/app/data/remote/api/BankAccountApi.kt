package com.billage.app.data.remote.api

import com.billage.app.ui.home.BankAccountInfo
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.POST

interface BankAccountApi {

    // 계좌 보유 여부 확인
    @GET("api/v1/bank/me")
    suspend fun checkBankAccount(): BankMeBaseResponse

    // 계좌 생성
    @POST("api/v1/bank/accounts")
    suspend fun createBankAccount(): BankAccountCreateBaseResponse

    // 계좌 목록 조회
    @GET("api/v1/bank/accounts")
    suspend fun getBankAccounts(): BankAccountListBaseResponse

    // 계좌 단건 조회 + 잔액 조회
    @GET("api/v1/bank/accounts/detail")
    suspend fun getAccountDetail(): BankAccountDetailBaseResponse
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

// data 필드는 사용하지 않으므로 역직렬화 생략 (ignoreUnknownKeys = true)
@Serializable
data class BankAccountCreateBaseResponse(
    val status: Int,
    val message: String
)

@Serializable
data class BankAccountListBaseResponse(
    val status: Int,
    val message: String,
    val data: List<BankAccountInfo>
)

@Serializable
data class BankAccountDetailBaseResponse(
    val status: Int,
    val message: String,
    val data: BankAccountInfo
)

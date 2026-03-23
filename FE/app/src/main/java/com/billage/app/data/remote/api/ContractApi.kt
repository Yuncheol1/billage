package com.billage.app.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface ContractApi {

    // 차용증 초안 생성
    @POST("api/v1/contracts")
    suspend fun createContractDraft(
        @Body request: CreateContractRequest
    ): CreateContractResponse

    // 특약 추가
    @POST("api/v1/contracts/{contractSeq}/clauses")
    suspend fun addClause(
        @Path("contractSeq") contractSeq: Int,
        @Body request: AddClauseRequest
    ): AddClauseResponse

    // 계약 목록 조회
    @GET("/api/v1/contracts")
    suspend fun getContracts(
        @Query("role") role: String? = "ALL",
        @Query("status") status: String? = null
    ): ContractListBaseResponse
}

// ─────────────────────────────────────────
// 차용증 초안 생성 Request
// ─────────────────────────────────────────
@Serializable
data class CreateContractRequest(
    val family: Boolean,              // "FAMILY" 또는 "NOT_FAMILY"
    val creditorUserSeq: Int,        // 채권자(빌려주는 사람) PK
    val debtorUserSeq: Int,          // 채무자(빌리는 사람) PK
    val principalAmount: Long,       // 원금 (0보다 커야 함)
    val interestRate: Double,        // 이자율 (예: 4.6% → 0.046)
    val repaymentMethod: String,     // "LUMP_SUM" | "MONTHLY" | "QUARTERLY" | "SEMI_ANNUAL" | "ANNUAL"
    val totalInstallments: Int?,     // 총 상환 횟수 (LUMP_SUM이면 null)
    val repaymentDay: Int,           // 상환일 (1~31)
    @SerialName("startDt")
    val startDt: String,             // 시작일 (예: "2026-03-10")
    @SerialName("endDt")
    val endDt: String                // 만기일 (예: "2027-03-09")
)

// ─────────────────────────────────────────
// 차용증 초안 생성 Response
// ─────────────────────────────────────────
@Serializable
data class CreateContractResponse(
    val status: Int,
    val message: String,
    val data: CreateContractData?
)

@Serializable
data class CreateContractData(
    val contractSeq: Int,        // 생성된 계약 PK — 특약 추가 시 사용
    val contractId: String       // 계약 ID (예: "C-20260317-A1B2C3")
)

// ─────────────────────────────────────────
// 특약 추가 Request / Response
// ─────────────────────────────────────────
@Serializable
data class AddClauseRequest(
    val clauseContent: String
)

@Serializable
data class AddClauseResponse(
    val status: Int,
    val message: String
)

// ─────────────────────────────────────────
// 계약 목록 조회 Response
// ─────────────────────────────────────────
@Serializable
data class ContractListBaseResponse(
    val status: Int,
    val message: String,
    val data: List<ContractListResponse>
)

@Serializable
data class ContractListResponse(
    val contractSeq: Int,
    val contractId: String? = null,
    val role: String? = null,
    val counterpartyName: String? = null,
    val principalAmount: Double? = null,
    val repaymentMethod: String? = null,
    val status: String? = null,
    val startDt: String? = null,
    val endDt: String? = null
)

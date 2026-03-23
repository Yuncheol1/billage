package com.billage.app.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.serialization.Serializable

interface ContractApi {
    @GET("/api/v1/contracts")
    suspend fun getContracts(
        @Query("role") role: String? = "ALL",
        @Query("status") status: String? = null
    ): ContractListBaseResponse
    @GET("/api/v1/contracts/{contractSeq}")
    suspend fun getContractDetail(
        @retrofit2.http.Path("contractSeq") contractSeq: Int
    ): ContractDetailBaseResponse

    @retrofit2.http.PUT("/api/v1/contracts/{contractSeq}")
    suspend fun updateContract(
        @retrofit2.http.Path("contractSeq") contractSeq: Int,
        @retrofit2.http.Body request: ContractUpdateRequest
    ): ContractupdateBaseResponse

    @retrofit2.http.POST("/api/v1/contracts/{contractSeq}/ai-propose-mock")
    suspend fun createAiProposalMock(
        @retrofit2.http.Path("contractSeq") contractSeq: Int
    ): ContractupdateBaseResponse
}

@Serializable
data class ContractupdateBaseResponse(
    val status: Int,
    val message: String
)

@Serializable
data class ContractUpdateRequest(
    val principalAmount: Double?,
    val interestRate: Double?,
    val clauses: List<ClauseUpdateDto>?
)

@Serializable
data class ClauseUpdateDto(
    val clauseSeq: Int,
    val content: String
)

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

@Serializable
data class ContractDetailBaseResponse(
    val status: Int,
    val message: String,
    val data: ContractDetailResponse
)

@Serializable
data class ContractDetailResponse(
    val contractSeq: Int,
    val contractId: String?,
    val family: Boolean?,
    val myRole: String?,
    val counterpartyName: String?,
    val principalAmount: Double?,
    val interestRate: Double?,
    val repaymentMethod: String?,
    val totalInstallments: Int?,
    val status: String?,
    val startDt: String?,
    val endDt: String?,
    val aiDraftContent: String? = null,
    val signedDttm: String? = null,
    val clauses: List<ClauseDetailDto> = emptyList()
)

@Serializable
data class ClauseDetailDto(
    val clauseSeq: Int,
    val title: String,
    val content: String,
    val orderNo: Int
)

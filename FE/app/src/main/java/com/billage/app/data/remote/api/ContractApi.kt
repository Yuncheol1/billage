package com.billage.app.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    // 자연어 → AI 변환 특약 추가
    @POST("api/v1/contracts/{contractSeq}/clausesAI")
    suspend fun addClauseAI(
        @Path("contractSeq") contractSeq: Int,
        @Body request: AddAiClauseRequest
    ): AddAiClauseResponse

    // 특약 삭제
    @DELETE("api/v1/contracts/{contractSeq}/clauses/{clauseSeq}")
    suspend fun deleteClause(
        @Path("contractSeq") contractSeq: Int,
        @Path("clauseSeq") clauseSeq: Int
    ): DeleteClauseResponse

    // 계약 목록 조회
    @GET("/api/v1/contracts")
    suspend fun getContracts(
        @Query("role") role: String? = "ALL",
        @Query("status") status: String? = null
    ): ContractListBaseResponse
    @GET("/api/v1/contracts/{contractSeq}")
    suspend fun getContractDetail(
        @Path("contractSeq") contractSeq: Int
    ): ContractDetailBaseResponse

    @retrofit2.http.PUT("/api/v1/contracts/{contractSeq}")
    suspend fun updateContract(
        @Path("contractSeq") contractSeq: Int,
        @Body request: ContractUpdateRequest,
        @Query("roomId") roomId: String? = null
    ): ContractupdateBaseResponse

    @POST("/api/v1/contracts/{contractSeq}/ai-propose-mock")
    suspend fun createAiProposalMock(
        @Path("contractSeq") contractSeq: Int
    ): ContractupdateBaseResponse

    // 계약 취소/거절 (서명 전 DRAFT/NEGOTIATING 상태만 가능)
    @DELETE("/api/v1/contracts/{contractSeq}")
    suspend fun deleteContract(
        @Path("contractSeq") contractSeq: Int
    ): DeleteContractResponse

    @retrofit2.http.PUT("api/v1/contracts/{contractSeq}/accept")
    suspend fun acceptContract(
        @Path("contractSeq") contractSeq: Int,
        @Query("roomId") roomId: String
    ): ContractDetailBaseResponse

    // 차용증 HTML 미리보기
    @GET("api/v1/contracts/{contractSeq}/document/html")
    suspend fun getContractDocumentHtml(
        @Path("contractSeq") contractSeq: Int
    ): ResponseBody

    @POST("/api/v1/contracts/{contractSeq}/signatures")
    suspend fun submitSignature(
        @Path("contractSeq") contractSeq: Int,
        @Body request: ContractSignatureRequest
    ): ContractSignatureBaseResponse

    @GET("/api/v1/contracts/{contractSeq}/ai-analysis")
    suspend fun getAiAnalysis(
        @Path("contractSeq") contractSeq: Int
    ): AiAnalysisResponse
    @POST("api/v1/contracts/{contractSeq}/clauses/from-chat")
    suspend fun generateClausesFromChat(
        @Path("contractSeq") contractSeq: Int,
        @Query("roomId") roomId: String
    ): AiClauseChatBaseResponse
}

@Serializable
data class AiAnalysisResponse(
    val status: Int,
    val message: String,
    val data: String
)

@Serializable
data class DeleteContractResponse(
    val status: Int,
    val message: String
)

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
    val totalInstallments: Int?,     // 총 상환 횟수 (LUMP_SUM의 경우 이자 납부 횟수)
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
    val message: String,
    val data: AddClauseData? = null
)

@Serializable
data class AddClauseData(
    val clauseSeq: Int   // 삭제 API 호출 시 필요한 특약 PK
)

@Serializable
data class DeleteClauseResponse(
    val status: Int,
    val message: String
)

@Serializable
data class AddAiClauseRequest(
    val clauseContent: String
)

@Serializable
data class AddAiClauseResponse(
    val status: Int,
    val message: String,
    val data: AddAiClauseData? = null
)

@Serializable
data class AddAiClauseData(
    val clauseSeq: Int,
    val content: String   // AI가 변환한 법적 문구
)

// ─────────────────────────────────────────
// 계약 목록 조회 Response
// ─────────────────────────────────────────
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
    val interestRate: Double? = null,
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
    val myRealName: String? = null,
    val counterpartyName: String?,
    val counterpartyProfileImageUrl: String? = null,
    val principalAmount: Double?,
    val interestRate: Double?,
    val repaymentMethod: String?,
    val totalInstallments: Int?,
    val status: String?,
    val startDt: String?,
    val endDt: String?,
    val aiDraftContent: String? = null,
    val signedDttm: String? = null,
    val isMeAccepted: Boolean? = false,
    val isOpponentAccepted: Boolean? = false,
    val isMeSigned: Boolean? = false,
    val isOpponentSigned: Boolean? = false,
    val roomId: String? = null,
    val counterpartyUserSeq: Int? = null,
    val clauses: List<ClauseDetailDto> = emptyList()
)

@Serializable
data class ClauseDetailDto(
    val clauseSeq: Int,
    val title: String,
    val content: String,
    val orderNo: Int
)

@Serializable
data class ContractSignatureRequest(
    val method: String,
    val deviceIdHash: String,
    val biometricVerified: Boolean,
    val userAgent: String,
    val publicKeyPem: String? = null,
    val signaturePayload: JsonObject,
    val signatureValue: String? = null
)

@Serializable
data class ContractSignatureBaseResponse(
    val status: Int,
    val message: String,
    val data: ContractSignatureResponse
)

@Serializable
data class ContractSignatureResponse(
    val signatureSeq: Int,
    val userSeq: Int,
    val role: String,
    val contractFullySigned: Boolean,
    val generatedDocumentSeq: Int? = null
)

@Serializable
data class AiClauseChatBaseResponse(
    val status: Int,
    val message: String,
    val data: AiClauseChatResponse
)

@Serializable
data class AiClauseChatResponse(
    val rejected: Boolean,
    val rejection_reason: String? = null,
    val clauses: List<AiClauseResponse>? = null,
    val summary: String? = null
)

@Serializable
data class AiClauseResponse(
    val title: String? = null,
    @SerialName("clause_id") val clauseId: String? = null,
    @SerialName("legal_clause") val legalClause: String? = null,
    val explanation: String? = null
)

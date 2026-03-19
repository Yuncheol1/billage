package com.billage.app.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ContractApi {

    // 특약 추가 — 자연어 입력 → AI가 법적 문구로 변환 후 저장
    // contractSeq: 계약 생성 시 응답받은 계약 PK
    @POST("api/v1/contracts/{contractSeq}/clauses")
    suspend fun addClause(
        @Path("contractSeq") contractSeq: Int,
        @Body request: AddClauseRequest
    ): AddClauseResponse
}

// 요청 본문 — 사용자가 자연어로 입력한 특약 내용
data class AddClauseRequest(
    val clauseContent: String
)

// 응답 본문 — 저장 성공 여부만 반환 (변환된 문구는 반환 안 됨)
data class AddClauseResponse(
    val status: Int,
    val message: String
)
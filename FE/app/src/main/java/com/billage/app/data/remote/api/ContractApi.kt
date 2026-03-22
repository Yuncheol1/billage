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
}

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

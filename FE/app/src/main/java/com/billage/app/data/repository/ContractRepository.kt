package com.billage.app.data.repository

import com.billage.app.data.remote.api.ContractApi
import com.billage.app.data.remote.api.ContractDetailResponse
import com.billage.app.data.remote.api.ContractListResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContractRepository @Inject constructor(
    private val contractApi: ContractApi
) {
    suspend fun getContracts(role: String? = "ALL", status: String? = null): List<ContractListResponse> {
        val response = contractApi.getContracts(role, status)
        return response.data
    }

    suspend fun getContractDetail(contractSeq: Int): ContractDetailResponse {
        val response = contractApi.getContractDetail(contractSeq)
        return response.data
    }

    suspend fun updateContract(contractSeq: Int, request: com.billage.app.data.remote.api.ContractUpdateRequest): com.billage.app.data.remote.api.ContractupdateBaseResponse {
        return contractApi.updateContract(contractSeq, request)
    }

    suspend fun createAiProposalMock(contractSeq: Int): com.billage.app.data.remote.api.ContractupdateBaseResponse {
        return contractApi.createAiProposalMock(contractSeq)
    }
}

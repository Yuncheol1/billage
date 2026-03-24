package com.billage.app.ui.contract.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.ContractDetailResponse
import com.billage.app.data.repository.ContractRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContractDetailViewModel @Inject constructor(
    private val contractRepository: ContractRepository
) : ViewModel() {

    private val _contractDetail = MutableStateFlow<ContractDetailResponse?>(null)
    val contractDetail: StateFlow<ContractDetailResponse?> = _contractDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchContractDetail(contractSeq: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val detail = contractRepository.getContractDetail(contractSeq)
                _contractDetail.value = detail
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "계약 상세 정보를 불러오지 못했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _updateSuccess = MutableSharedFlow<Boolean>()
    val updateSuccess: SharedFlow<Boolean> = _updateSuccess

    fun updateContract(
        contractSeq: Int,
        principalAmount: Double?,
        interestRate: Double?,
        clauses: List<com.billage.app.data.remote.api.ClauseUpdateDto>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = com.billage.app.data.remote.api.ContractUpdateRequest(
                    principalAmount = principalAmount,
                    interestRate = interestRate,
                    clauses = clauses
                )
                contractRepository.updateContract(contractSeq, request)
                _updateSuccess.emit(true)
                // 성공적으로 업데이트 후 상세 정보를 다시 불러옵니다
                fetchContractDetail(contractSeq)
            } catch (e: Exception) {
                _errorMessage.value = "계약 수정에 실패했습니다: ${e.message}"
                _updateSuccess.emit(false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

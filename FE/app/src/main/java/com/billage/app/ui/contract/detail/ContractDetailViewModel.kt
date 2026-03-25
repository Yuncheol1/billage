package com.billage.app.ui.contract.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.PdfDownloader
import com.billage.app.data.remote.api.ClauseUpdateDto
import com.billage.app.data.remote.api.ContractDetailResponse
import com.billage.app.data.remote.api.ContractUpdateRequest
import com.billage.app.data.remote.api.UserApi
import com.billage.app.data.repository.ContractRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PdfDownloadState {
    object Idle : PdfDownloadState()
    object Loading : PdfDownloadState()
    object Success : PdfDownloadState()
    object Error : PdfDownloadState()
}

@HiltViewModel
class ContractDetailViewModel @Inject constructor(
    private val contractRepository: ContractRepository,
    private val userApi: UserApi,
    private val pdfDownloader: PdfDownloader
) : ViewModel() {

    private val _contractDetail = MutableStateFlow<ContractDetailResponse?>(null)
    val contractDetail: StateFlow<ContractDetailResponse?> = _contractDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _myName = MutableStateFlow("나")
    val myName: StateFlow<String> = _myName.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { userApi.getMyInfo().data.realName }
                .onSuccess { name -> if (!name.isNullOrBlank()) _myName.value = name }
        }
    }

    fun fetchContractDetail(contractSeq: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _contractDetail.value = contractRepository.getContractDetail(contractSeq)
            } catch (e: Exception) {
                _errorMessage.value = "계약 상세 정보를 불러오지 못했습니다"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _contractHtml = MutableStateFlow<String?>(null)
    val contractHtml: StateFlow<String?> = _contractHtml.asStateFlow()

    fun fetchContractHtml(contractSeq: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _contractHtml.value = contractRepository.getContractHtml(contractSeq)
            } catch (e: Exception) {
                _errorMessage.value = "차용증 문서를 불러오지 못했습니다"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _pdfDownloadState = MutableStateFlow<PdfDownloadState>(PdfDownloadState.Idle)
    val pdfDownloadState: StateFlow<PdfDownloadState> = _pdfDownloadState.asStateFlow()

    fun downloadPdf(contractSeq: Int) {
        viewModelScope.launch {
            _pdfDownloadState.value = PdfDownloadState.Loading
            _pdfDownloadState.value = pdfDownloader.download(contractSeq).fold(
                onSuccess = { PdfDownloadState.Success },
                onFailure = { PdfDownloadState.Error }
            )
        }
    }

    fun clearPdfDownloadState() {
        _pdfDownloadState.value = PdfDownloadState.Idle
    }

    private val _updateSuccess = MutableSharedFlow<Boolean>()
    val updateSuccess: SharedFlow<Boolean> = _updateSuccess

    fun updateContract(
        contractSeq: Int,
        principalAmount: Double?,
        interestRate: Double?,
        clauses: List<ClauseUpdateDto>,
        roomId: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = ContractUpdateRequest(
                    principalAmount = principalAmount,
                    interestRate = interestRate,
                    clauses = clauses
                )
                contractRepository.updateContract(
                    contractSeq,
                    ContractUpdateRequest(
                        principalAmount = principalAmount,
                        interestRate = interestRate,
                        clauses = clauses
                    )
                )
                contractRepository.updateContract(contractSeq, request, roomId)
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

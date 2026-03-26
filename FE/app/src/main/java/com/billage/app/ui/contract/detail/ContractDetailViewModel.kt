package com.billage.app.ui.contract.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.PdfDownloader
import com.billage.app.data.remote.api.ClauseUpdateDto
import com.billage.app.data.remote.api.ContractDetailResponse
import com.billage.app.data.remote.api.ContractUpdateRequest
import com.billage.app.data.remote.api.RepaymentScheduleItem
import com.billage.app.data.remote.api.UserApi
import com.billage.app.data.repository.ContractRepository
import com.billage.app.data.repository.RepaymentRepository
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
    private val repaymentRepository: RepaymentRepository,
    private val userApi: UserApi,
    private val pdfDownloader: PdfDownloader
) : ViewModel() {

    private val _contractDetail = MutableStateFlow<ContractDetailResponse?>(null)
    val contractDetail: StateFlow<ContractDetailResponse?> = _contractDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _repaymentSchedules = MutableStateFlow<List<RepaymentScheduleItem>>(emptyList())
    val repaymentSchedules: StateFlow<List<RepaymentScheduleItem>> = _repaymentSchedules.asStateFlow()

    private val _isTimelineLoading = MutableStateFlow(false)
    val isTimelineLoading: StateFlow<Boolean> = _isTimelineLoading.asStateFlow()

    private val _timelineErrorMessage = MutableStateFlow<String?>(null)
    val timelineErrorMessage: StateFlow<String?> = _timelineErrorMessage.asStateFlow()

    private val _activeActionScheduleSeq = MutableStateFlow<Int?>(null)
    val activeActionScheduleSeq: StateFlow<Int?> = _activeActionScheduleSeq.asStateFlow()

    private val _myName = MutableStateFlow("나")
    val myName: StateFlow<String> = _myName.asStateFlow()

    private val _myProfileImageUrl = MutableStateFlow<String?>(null)
    val myProfileImageUrl: StateFlow<String?> = _myProfileImageUrl.asStateFlow()

    private val _contractHtml = MutableStateFlow<String?>(null)
    val contractHtml: StateFlow<String?> = _contractHtml.asStateFlow()

    private val _pdfDownloadState = MutableStateFlow<PdfDownloadState>(PdfDownloadState.Idle)
    val pdfDownloadState: StateFlow<PdfDownloadState> = _pdfDownloadState.asStateFlow()

    private val _updateSuccess = MutableSharedFlow<Boolean>()
    val updateSuccess: SharedFlow<Boolean> = _updateSuccess

    init {
        viewModelScope.launch {
            runCatching { userApi.getMyInfo().data }
                .onSuccess { user ->
                    if (!user.realName.isNullOrBlank()) {
                        _myName.value = user.realName
                    }
                    _myProfileImageUrl.value = user.profileImageUrl
                }
        }
    }

    fun fetchContractDetail(contractSeq: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _contractDetail.value = contractRepository.getContractDetail(contractSeq)
            } catch (_: Exception) {
                _errorMessage.value = "계약 상세 정보를 불러오지 못했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchRepaymentSchedules(contractSeq: Int) {
        viewModelScope.launch {
            _isTimelineLoading.value = true
            _timelineErrorMessage.value = null
            try {
                _repaymentSchedules.value = repaymentRepository.getRepaymentSchedules(contractSeq)
            } catch (_: Exception) {
                _repaymentSchedules.value = emptyList()
                _timelineErrorMessage.value = "상환 타임라인을 불러오지 못했습니다."
            } finally {
                _isTimelineLoading.value = false
            }
        }
    }

    fun markRepaymentSchedulePaid(contractSeq: Int, scheduleSeq: Int) {
        viewModelScope.launch {
            _activeActionScheduleSeq.value = scheduleSeq
            _timelineErrorMessage.value = null
            try {
                repaymentRepository.markRepaymentSchedulePaid(contractSeq, scheduleSeq)
                fetchContractDetail(contractSeq)
                fetchRepaymentSchedules(contractSeq)
            } catch (_: Exception) {
                _timelineErrorMessage.value = "수동 상환 처리에 실패했습니다."
            } finally {
                _activeActionScheduleSeq.value = null
            }
        }
    }

    fun transferRepaymentSchedule(contractSeq: Int, scheduleSeq: Int) {
        viewModelScope.launch {
            _activeActionScheduleSeq.value = scheduleSeq
            _timelineErrorMessage.value = null
            try {
                repaymentRepository.transferRepaymentSchedule(contractSeq, scheduleSeq)
                fetchContractDetail(contractSeq)
                fetchRepaymentSchedules(contractSeq)
            } catch (_: Exception) {
                _timelineErrorMessage.value = "바로 상환 처리에 실패했습니다."
            } finally {
                _activeActionScheduleSeq.value = null
            }
        }
    }

    fun fetchContractHtml(contractSeq: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _contractHtml.value = contractRepository.getContractHtml(contractSeq)
            } catch (_: Exception) {
                _errorMessage.value = "차용증 문서를 불러오지 못했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }

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

    fun updateContract(
        contractSeq: Int,
        principalAmount: Double?,
        interestRate: Double?,
        clauses: List<ClauseUpdateDto>,
        roomId: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val request = ContractUpdateRequest(
                    principalAmount = principalAmount,
                    interestRate = interestRate,
                    clauses = clauses
                )
                contractRepository.updateContract(contractSeq, request, roomId)
                _updateSuccess.emit(true)
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

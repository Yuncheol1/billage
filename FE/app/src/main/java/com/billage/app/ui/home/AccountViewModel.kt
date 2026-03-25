package com.billage.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.BankAccountApi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class AccountScreenState {
    object Loading : AccountScreenState()
    data class HasAccount(
        val account: BankAccountDto,
        val transactions: List<TransactionDto> = mockTransactions
    ) : AccountScreenState()
    object NoAccount : AccountScreenState()
    data class Error(val message: String) : AccountScreenState()
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val bankAccountApi: BankAccountApi
) : ViewModel() {

    private val _state = MutableStateFlow<AccountScreenState>(AccountScreenState.Loading)
    val state: StateFlow<AccountScreenState> = _state.asStateFlow()

    private val _transactionDetail = MutableStateFlow<TransactionDto?>(null)
    val transactionDetail: StateFlow<TransactionDto?> = _transactionDetail.asStateFlow()

    private val _transactionDetailLoading = MutableStateFlow(false)
    val transactionDetailLoading: StateFlow<Boolean> = _transactionDetailLoading.asStateFlow()

    init {
        checkAccount()
    }

    fun checkAccount() {
        viewModelScope.launch {
            _state.value = AccountScreenState.Loading
            try {
                val response = bankAccountApi.checkBankAccount()
                if (response.data.hasBankAccount) {
                    loadAccountInfo()
                } else {
                    _state.value = AccountScreenState.NoAccount
                }
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    _state.value = AccountScreenState.NoAccount
                } else {
                    _state.value = AccountScreenState.Error(e.toKoreanMessage("계좌 확인"))
                }
            } catch (e: Exception) {
                _state.value = AccountScreenState.Error(e.toKoreanMessage("계좌 확인"))
            }
        }
    }

    fun createAccount() {
        viewModelScope.launch {
            _state.value = AccountScreenState.Loading
            try {
                bankAccountApi.createBankAccount()
                loadAccountInfo()
            } catch (e: Exception) {
                _state.value = AccountScreenState.Error(e.toKoreanMessage("계좌 생성"))
            }
        }
    }

    fun loadTransactionDetail(transactionUniqueNo: Long) {
        viewModelScope.launch {
            _transactionDetailLoading.value = true
            _transactionDetail.value = null
            try {
                val response = bankAccountApi.getTransactionDetail(transactionUniqueNo)
                _transactionDetail.value = response.data
            } catch (e: Exception) {
                // API 실패 시 목데이터에서 fallback
                _transactionDetail.value = mockTransactions.find { it.transactionUniqueNo == transactionUniqueNo }
            } finally {
                _transactionDetailLoading.value = false
            }
        }
    }

    private suspend fun loadAccountInfo() {
        try {
            val fmt = DateTimeFormatter.ofPattern("yyyyMMdd")
            val today = LocalDate.now()
            val startDate = today.minusYears(1).format(fmt)
            val endDate = today.format(fmt)

            val detailResponse = bankAccountApi.getAccountDetail()
            val txResponse = bankAccountApi.getTransactions(startDate, endDate)
            _state.value = AccountScreenState.HasAccount(
                account = detailResponse.data,
                transactions = txResponse.data.list.ifEmpty { mockTransactions }
            )
        } catch (e: Exception) {
            _state.value = AccountScreenState.Error(e.toKoreanMessage("계좌 조회"))
        }
    }

    private fun Exception.toKoreanMessage(context: String): String = when {
        message?.contains("required") == true || message?.contains("missing") == true ->
            "${context} 중 서버 응답 형식 오류가 발생했습니다.\n(${this.javaClass.simpleName})"
        message?.contains("Unable to resolve host") == true ||
        message?.contains("timeout") == true ->
            "서버에 연결할 수 없습니다. 인터넷 연결을 확인해주세요."
        message?.contains("401") == true ->
            "인증이 만료되었습니다. 다시 로그인해주세요."
        message?.contains("500") == true ->
            "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
        else ->
            "${context} 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
    }
}

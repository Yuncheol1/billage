package com.billage.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.BankAccountApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AccountScreenState {
    object Loading : AccountScreenState()
    data class HasAccount(
        val account: BankAccountInfo,
        val transactions: List<Transaction> = mockTransactions
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

    private suspend fun loadAccountInfo() {
        try {
            val listResponse = bankAccountApi.getBankAccounts()
            // isPrimary 계좌 우선, 없으면 첫 번째 계좌
            val account = listResponse.data.firstOrNull { it.isPrimary }
                ?: listResponse.data.firstOrNull()
            if (account == null) {
                _state.value = AccountScreenState.NoAccount
                return
            }
            val balanceResponse = bankAccountApi.getAccountBalance()
            _state.value = AccountScreenState.HasAccount(
                account = account.copy(accountBalance = balanceResponse.data.accountBalance)
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

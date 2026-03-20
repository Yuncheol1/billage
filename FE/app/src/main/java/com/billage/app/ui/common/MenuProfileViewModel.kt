package com.billage.app.ui.common

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.UserApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MenuProfileState(
    val realName: String = "",
    val email: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class MenuProfileViewModel @Inject constructor(
    private val userApi: UserApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuProfileState(isLoading = true))
    val uiState: StateFlow<MenuProfileState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching { userApi.getMyInfo().data }
                .onSuccess { user ->
                    _uiState.value = MenuProfileState(
                        realName = user.realName.orEmpty(),
                        email = user.email.orEmpty(),
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    Log.e("MenuProfileViewModel", "Failed to fetch my profile", error)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
        }
    }
}

package com.billage.app.ui.contract.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.AddClauseRequest
import com.billage.app.data.remote.api.ContractApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SpecialTerm(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isRecommended: Boolean = false,
    val clauseSeq: Int = -1  // 서버 PK — 삭제 API 호출 시 필요
)

data class RecommendedTermItem(
    val emoji: String,
    val label: String,
    val clauseContent: String
)

data class SpecialTermsUiState(
    val addedTerms: List<SpecialTerm> = emptyList(),
    val directInputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ContractSpecialTermsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contractApi: ContractApi
) : ViewModel() {

    val contractSeq: Int = savedStateHandle.get<String>("contractSeq")?.toIntOrNull() ?: -1

    private val _uiState = MutableStateFlow(SpecialTermsUiState())
    val uiState: StateFlow<SpecialTermsUiState> = _uiState.asStateFlow()

    val recommendedTerms = listOf(
        RecommendedTermItem("⏩", "중도 상환 허용", "중도 상환을 허용한다"),
        RecommendedTermItem("⏰", "연체 이자 부과", "연체 시 연체이자를 부과한다"),
        RecommendedTermItem("💳", "계좌이체로만 상환", "상환은 계좌이체로만 한다"),
        RecommendedTermItem("🔒", "채권 양도 금지", "채권 양도를 금지한다"),
        RecommendedTermItem("🤫", "계약 내용 비밀 유지", "계약 내용을 비밀로 유지한다"),
        RecommendedTermItem("⚖️", "분쟁 시 조정 우선", "분쟁 발생 시 조정을 우선한다")
    )

    fun addRecommendedTerm(item: RecommendedTermItem) {
        val alreadyAdded = _uiState.value.addedTerms.any {
            it.title == item.label && it.isRecommended
        }
        if (alreadyAdded) return

        callAddClauseApi(
            clauseContent = item.clauseContent,
            displayTitle = item.label,
            isRecommended = true
        )
    }

    fun updateDirectInputText(text: String) {
        _uiState.value = _uiState.value.copy(
            directInputText = text,
            errorMessage = null  // 타이핑 시 에러 초기화
        )
    }

    fun addDirectInputTerm() {
        val content = _uiState.value.directInputText.trim()

        // 유효성 검사
        if (content.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "내용을 입력해주세요")
            return
        }
        if (content.length < 10) {
            _uiState.value = _uiState.value.copy(errorMessage = "조금 더 구체적으로 입력해주세요")
            return
        }

        callAddClauseApi(
            clauseContent = content,
            displayTitle = content,
            isRecommended = false
        )
    }

    fun removeTerm(termId: String) {
        val term = _uiState.value.addedTerms.find { it.id == termId } ?: return

        // clauseSeq가 없으면 로컬에서만 제거 (서버에 저장 안 된 항목)
        if (contractSeq == -1 || term.clauseSeq == -1) {
            _uiState.value = _uiState.value.copy(
                addedTerms = _uiState.value.addedTerms.filter { it.id != termId }
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = contractApi.deleteClause(
                    contractSeq = contractSeq,
                    clauseSeq = term.clauseSeq
                )
                if (response.status == 200) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        addedTerms = _uiState.value.addedTerms.filter { it.id != termId }
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = response.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "특약 삭제에 실패했어요. 다시 시도해 주세요."
                )
            }
        }
    }

    private fun callAddClauseApi(
        clauseContent: String,
        displayTitle: String,
        isRecommended: Boolean
    ) {
        if (contractSeq == -1) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "계약 정보를 찾을 수 없어요."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            try {
                val response = contractApi.addClause(
                    contractSeq = contractSeq,
                    request = AddClauseRequest(clauseContent = clauseContent)
                )

                if (response.status == 201) {
                    val clauseSeq = response.data?.clauseSeq ?: -1
                    val newTerm = SpecialTerm(
                        title = displayTitle,
                        isRecommended = isRecommended,
                        clauseSeq = clauseSeq
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        addedTerms = _uiState.value.addedTerms + newTerm,
                        directInputText = if (!isRecommended) "" else _uiState.value.directInputText
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "특약 생성에 실패했어요. 다시 시도해주세요"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "특약 생성에 실패했어요. 다시 시도해주세요"
                )
            }
        }
    }
}

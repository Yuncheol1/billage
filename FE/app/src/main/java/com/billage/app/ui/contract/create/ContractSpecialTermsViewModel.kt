package com.billage.app.ui.contract.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.AddAiClauseRequest
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
        RecommendedTermItem("⏩", "중도 상환 허용", "채무자는 만기일 이전에도 원금의 전부 또는 일부를 중도 상환할 수 있으며, 이 경우 중도상환수수료는 부과하지 않는다."),
        RecommendedTermItem("⏰", "연체 이자 부과", "채무자가 약정된 상환일로부터 지체할 경우, 연체된 금액에 대해 연체 기간 동안 연 20%의 연체이자를 부과한다."),
        RecommendedTermItem("💳", "계좌이체로만 상환", "모든 상환은 채권자 명의의 지정 계좌로 계좌이체 방식으로만 진행하며, 현금 수령은 인정하지 않는다."),
        RecommendedTermItem("🔒", "채권 양도 금지", "채권자는 본 계약에 따른 채권을 채무자의 서면 동의 없이 제3자에게 양도하거나 담보로 제공할 수 없다."),
        RecommendedTermItem("🤫", "계약 내용 비밀 유지", "양 당사자는 본 계약의 내용을 제3자에게 공개하지 않기로 하며, 이를 위반할 경우 손해배상 책임을 진다."),
        RecommendedTermItem("⚖️", "분쟁 시 조정 우선", "본 계약과 관련한 분쟁이 발생할 경우, 소송 제기 전에 한국소비자원 또는 대한상사중재원의 조정 절차를 먼저 거친다.")
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
        val clauseContent = _uiState.value.directInputText.trim()

        if (clauseContent.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "내용을 입력해주세요")
            return
        }

        if (contractSeq == -1) {
            _uiState.value = _uiState.value.copy(errorMessage = "계약 정보를 찾을 수 없어요.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = contractApi.addClauseAI(
                    contractSeq = contractSeq,
                    request = AddAiClauseRequest(clauseContent = clauseContent)
                )
                if (response.status == 201) {
                    val aiContent = response.data?.content ?: clauseContent
                    val clauseSeq = response.data?.clauseSeq ?: -1
                    val newTerm = SpecialTerm(
                        title = aiContent,
                        isRecommended = false,
                        clauseSeq = clauseSeq
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        addedTerms = _uiState.value.addedTerms + newTerm,
                        directInputText = ""
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "AI 변환에 실패했어요. 다시 시도해주세요."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "AI 변환에 실패했어요. 다시 시도해주세요."
                )
            }
        }
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

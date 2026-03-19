package com.billage.app.ui.contract

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billage.app.data.remote.api.AddClauseRequest
import com.billage.app.data.remote.api.ContractApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// 화면에 표시할 특약 한 항목 (로컬 목록 표시용 — 이미 BE에 저장된 상태)
data class SpecialTerm(
    val id: String = UUID.randomUUID().toString(),
    val title: String,                              // 화면에 표시할 텍스트
    val isRecommended: Boolean = false              // 추천 특약 여부
)

// 추천 특약 버튼 항목 정의
data class RecommendedTermItem(
    val emoji: String,
    val label: String,
    val clauseContent: String  // API에 보낼 자연어 내용
)

// 화면 전체 상태
data class SpecialTermsUiState(
    val addedTerms: List<SpecialTerm> = emptyList(), // 추가 완료된 특약 목록 (UI 표시용)
    val directInputText: String = "",                // 직접 입력 텍스트
    val isLoading: Boolean = false,                  // API 호출 중 여부
    val errorMessage: String? = null                 // 에러 메시지
)

class ContractSpecialTermsViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 네비게이션에서 전달받은 계약 PK
    // TODO: 계약 생성 API 연동 후 실제 contractSeq 전달
    val contractSeq: Int = savedStateHandle.get<String>("contractSeq")?.toIntOrNull() ?: -1

    private val _uiState = MutableStateFlow(SpecialTermsUiState())
    val uiState: StateFlow<SpecialTermsUiState> = _uiState.asStateFlow()

    // TODO: Hilt DI 연동 후 주입 방식으로 교체
    // @Inject lateinit var contractApi: ContractApi

    // 와이어프레임 7/7 기준 추천 특약 목록
    val recommendedTerms = listOf(
        RecommendedTermItem("⏩", "중도 상환 허용", "중도 상환을 허용한다"),
        RecommendedTermItem("⏰", "연체 이자 부과", "연체 시 연체이자를 부과한다"),
        RecommendedTermItem("💳", "계좌이체로만 상환", "상환은 계좌이체로만 한다"),
        RecommendedTermItem("🔒", "채권 양도 금지", "채권 양도를 금지한다"),
        RecommendedTermItem("🤫", "계약 내용 비밀 유지", "계약 내용을 비밀로 유지한다"),
        RecommendedTermItem("⚖️", "분쟁 시 조정 우선", "분쟁 발생 시 조정을 우선한다")
    )

    // 추천 특약 버튼 탭 → 즉시 API 호출 후 목록에 추가
    fun addRecommendedTerm(item: RecommendedTermItem) {
        // 이미 추가된 항목은 중복 추가 방지
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

    // 직접 입력 텍스트 변경
    fun updateDirectInputText(text: String) {
        _uiState.value = _uiState.value.copy(
            directInputText = text,
            errorMessage = null
        )
    }

    // 직접 입력 추가 버튼 → 즉시 API 호출 후 목록에 추가
    fun addDirectInputTerm() {
        val content = _uiState.value.directInputText.trim()
        if (content.isEmpty()) return

        callAddClauseApi(
            clauseContent = content,
            displayTitle = content,
            isRecommended = false
        )
    }

    // 특약 삭제 — 로컬 목록에서만 제거
    // ⚠️ BE에는 삭제 API 없음 — 화면 표시용으로만 제거
    fun removeTerm(termId: String) {
        _uiState.value = _uiState.value.copy(
            addedTerms = _uiState.value.addedTerms.filter { it.id != termId }
        )
    }

    // 공통 API 호출 함수
    private fun callAddClauseApi(
        clauseContent: String,
        displayTitle: String,
        isRecommended: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            try {
                // TODO: Hilt DI 연동 후 실제 API 호출로 교체
                // contractApi.addClause(
                //     contractSeq = contractSeq,
                //     request = AddClauseRequest(clauseContent = clauseContent)
                // )

                // 임시 딜레이 (API 연동 전 더미 처리)
                kotlinx.coroutines.delay(500)

                // API 성공 시 로컬 목록에 추가
                val newTerm = SpecialTerm(
                    title = displayTitle,
                    isRecommended = isRecommended
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    addedTerms = _uiState.value.addedTerms + newTerm,
                    directInputText = if (!isRecommended) "" else _uiState.value.directInputText
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "특약 추가에 실패했어요. 다시 시도해 주세요."
                )
            }
        }
    }
}
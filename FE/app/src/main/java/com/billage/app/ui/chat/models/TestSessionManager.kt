package com.billage.app.ui.chat.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 프론트엔드 개발 및 테스트용 세션 매니저
 * 에뮬레이터/실기기 하드코딩 없이 앱 내에서 바로 사용자(100, 101등)를 변경할 수 있도록 합니다.
 */
object TestSessionManager {
    private val _myUserSeq = MutableStateFlow(100)
    val myUserSeq: StateFlow<Int> = _myUserSeq.asStateFlow()

    fun setUserSeq(seq: Int) {
        _myUserSeq.value = seq
    }
}

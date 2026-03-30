package com.billage.app.core.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface AppEvent {
    data class ContractDeleted(val contractSeq: Int) : AppEvent
}

object AppEventBus {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 32)
    val events = _events.asSharedFlow()

    fun emit(event: AppEvent) {
        _events.tryEmit(event)
    }
}

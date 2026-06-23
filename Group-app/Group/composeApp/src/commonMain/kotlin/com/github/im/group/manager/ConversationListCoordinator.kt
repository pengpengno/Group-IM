package com.github.im.group.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class ConversationListEvent(
    val conversationId: Long,
    val moveToTop: Boolean = false
)

class ConversationListCoordinator {
    private val _events = MutableSharedFlow<ConversationListEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<ConversationListEvent> = _events.asSharedFlow()

    fun notifyConversationChanged(conversationId: Long, moveToTop: Boolean = false) {
        _events.tryEmit(
            ConversationListEvent(
                conversationId = conversationId,
                moveToTop = moveToTop
            )
        )
    }
}

package com.github.im.group.manager

object AppRuntimeState {
    @Volatile
    private var foreground: Boolean = false

    @Volatile
    private var activeConversationId: Long? = null

    private val mutedConversationUntil: MutableMap<Long, Long> = linkedMapOf()

    fun markForeground() {
        foreground = true
    }

    fun markBackground() {
        foreground = false
    }

    fun setActiveConversation(conversationId: Long?) {
        activeConversationId = conversationId
    }

    fun clearActiveConversation(conversationId: Long? = null) {
        if (conversationId == null || activeConversationId == conversationId) {
            activeConversationId = null
        }
    }

    @Synchronized
    fun replaceMutedConversationState(conversationStates: Map<Long, Long>) {
        mutedConversationUntil.clear()
        mutedConversationUntil.putAll(conversationStates)
    }

    @Synchronized
    fun setConversationMutedUntil(conversationId: Long, muteUntil: Long) {
        if (muteUntil > 0L) {
            mutedConversationUntil[conversationId] = muteUntil
        } else {
            mutedConversationUntil.remove(conversationId)
        }
    }

    @Synchronized
    fun isConversationMuted(conversationId: Long?): Boolean {
        if (conversationId == null) return false
        val muteUntil = mutedConversationUntil[conversationId] ?: return false
        return muteUntil == Long.MAX_VALUE || muteUntil > currentTimeMillis()
    }

    @Synchronized
    fun getConversationMuteUntil(conversationId: Long?): Long {
        if (conversationId == null) return 0L
        return mutedConversationUntil[conversationId] ?: 0L
    }

    fun isForeground(): Boolean = foreground

    fun getActiveConversationId(): Long? = activeConversationId

    fun shouldShowSystemChatNotification(conversationId: Long?): Boolean {
        if (isConversationMuted(conversationId)) return false
        if (!foreground) return true
        return false
    }

    fun shouldShowRealtimeConversationHint(conversationId: Long): Boolean {
        if (isConversationMuted(conversationId)) return false
        return !(foreground && activeConversationId == conversationId)
    }

    private fun currentTimeMillis(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
}

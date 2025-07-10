package com.github.im.group.sdk

interface VoiceRecorder {
    fun startRecording(conversationId: Long)
    fun stopRecording(): VoiceRecordingResult?
}

data class VoiceRecordingResult(
    val bytes: ByteArray,
    val durationMillis: Long
)

expect object VoiceRecorderFactory {
    fun create(): VoiceRecorder
}
package com.github.im.group.sdk

interface VoiceRecorder {
    /**
     * 开始录音
     */
    fun startRecording(conversationId: Long)

    /**
     * 停止录音
     */
    fun stopRecording(): VoiceRecordingResult?

    /**
     * 获取 音量
     */
    fun getAmplitude():  Int   // 替代 getAmplitude()


    /**
     * 获取输出的文件路径
     */
    fun getOutputFile(): String?

    /**
     * 获取音频数据
     * 只有录音停止才会返回数据 ，正在录音则返回null
     */
    fun getVoiceData(): VoiceRecordingResult?

}

data class VoiceRecordingResult(
    val bytes: ByteArray,
    val durationMillis: Long
)

expect object VoiceRecorderFactory {
    fun create(): VoiceRecorder
}
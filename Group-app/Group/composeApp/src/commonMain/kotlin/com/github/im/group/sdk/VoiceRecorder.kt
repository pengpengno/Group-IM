package com.github.im.group.sdk

interface VoiceRecorder {
    /**
     * 开始录音
     */
    fun startRecording()

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
     * 只有录音停止才会返回数据 ，正在录音则返回 null
     */
    fun getVoiceData(): VoiceRecordingResult?

}

/**
 * 音频记录结果
 */
data class VoiceRecordingResult(
    val bytes: ByteArray,
    val durationMillis: Long,
    val file: File,
    val name: String,
    val filePath: String? = null  // 添加文件路径信息
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoiceRecordingResult

        if (durationMillis != other.durationMillis) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (name != other.name) return false
        if (filePath != other.filePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = durationMillis.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (filePath?.hashCode() ?: 0)
        return result
    }
}


fun VoiceRecordingResult.toFile(): File {
    return file
}


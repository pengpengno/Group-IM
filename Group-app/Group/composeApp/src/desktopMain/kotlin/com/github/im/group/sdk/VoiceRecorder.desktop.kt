package com.github.im.group.sdk

import io.github.aakira.napier.Napier

class DesktopVoiceRecorder : VoiceRecorder {
    private var outputFile: String? = null
    private var recordingResult: VoiceRecordingResult? = null

    override fun startRecording() {
        Napier.d("Desktop: Starting voice recording")
        // 实际录音逻辑将在后续开发中完成，可能使用Java Sound API
    }

    override fun stopRecording(): VoiceRecordingResult? {
        Napier.d("Desktop: Stopping voice recording")
        // 实际录音逻辑将在后续开发中完成
        return recordingResult
    }

    override fun getAmplitude(): Int {
        Napier.d("Desktop: Getting amplitude")
        // 实际实现将在后续开发中完成
        return 0
    }

    override fun getOutputFile(): String? {
        Napier.d("Desktop: Getting output file")
        return outputFile
    }

    override fun getVoiceData(): VoiceRecordingResult? {
        Napier.d("Desktop: Getting voice data")
        return recordingResult
    }
}
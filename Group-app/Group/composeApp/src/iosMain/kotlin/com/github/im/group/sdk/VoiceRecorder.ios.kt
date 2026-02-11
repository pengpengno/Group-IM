package com.github.im.group.sdk

import io.github.aakira.napier.Napier

class IosVoiceRecorder : VoiceRecorder {
    private var outputFile: String? = null
    private var recordingResult: VoiceRecordingResult? = null

    override fun startRecording() {
        Napier.d("iOS: Starting voice recording")
        // 实际录音逻辑将在后续开发中完成
    }

    override fun stopRecording(): VoiceRecordingResult? {
        Napier.d("iOS: Stopping voice recording")
        // 实际录音逻辑将在后续开发中完成
        return recordingResult
    }

    override fun getAmplitude(): Int {
        Napier.d("iOS: Getting amplitude")
        // 实际实现将在后续开发中完成
        return 0
    }

    override fun getOutputFile(): String? {
        Napier.d("iOS: Getting output file")
        return outputFile
    }

    override fun getVoiceData(): VoiceRecordingResult? {
        Napier.d("iOS: Getting voice data")
        return recordingResult
    }
}
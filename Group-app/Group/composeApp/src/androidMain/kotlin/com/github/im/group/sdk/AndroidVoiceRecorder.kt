package com.github.im.group.sdk
import android.content.Context
import android.media.MediaRecorder
import com.github.im.group.sdk.VoiceRecorderFactory.context
import java.io.File

class AndroidVoiceRecorder(private val context: Context) : VoiceRecorder {

    private var outputFile: File? = null
    private var recorder: MediaRecorder? = null
    private var startTime: Long = 0

     override fun startRecording(conversationId: Long) {
        outputFile = File.createTempFile("voice_${conversationId}_", ".m4a", context.cacheDir)
        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }
        startTime = System.currentTimeMillis()
    }

     override fun stopRecording(): VoiceRecordingResult? {
        val recorder = recorder ?: return null
        return try {
            recorder.stop()
            recorder.release()
            val bytes = outputFile?.readBytes() ?: return null
            val duration = System.currentTimeMillis() - startTime
            VoiceRecordingResult(bytes, duration)
        } catch (e: Exception) {
            null
        } finally {
            this.recorder = null
        }
    }
}

 fun create(): VoiceRecorder = AndroidVoiceRecorder(context)

//actual object VoiceRecorderFactory {
//    lateinit var context: Context
//    actual fun create(): VoiceRecorder = AndroidVoiceRecorder(context)
//}
actual object VoiceRecorderFactory {
    lateinit var context: Context
    actual fun create(): VoiceRecorder = AndroidVoiceRecorder(context)
}

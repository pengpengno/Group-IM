package com.github.im.group.sdk
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

class AndroidVoiceRecorder(private val context: Context) : VoiceRecorder {

    private var outputFile: File? = null
    private var recorder: MediaRecorder? = null
    private var startTime: Long = 0

     @RequiresApi(Build.VERSION_CODES.S)
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

///**
// * 音量可视化
// */
//@Composable
//fun VolumeVisualizer(volume: Int) {
//    // 直观：音量越大，圆越大
//    val size = 40.dp + (volume * 0.6f).dp
//    Box(
//        modifier = Modifier
//            .size(size)
//            .background(Color.Red, shape = CircleShape)
//    )
//}

//@Composable
//fun rememberVolumeLevel(recorder: MediaRecorder?): State<Int> {
//    val volumeLevel = remember { mutableStateOf(0) }
//
//    LaunchedEffect(recorder) {
//        while (recorder != null) {
//            val amp = try {
//                recorder.maxAmplitude
//            } catch (e: Exception) {
//                0
//            }
//            // Normalize to [0..100]
//            volumeLevel.value = (amp / 32767.0 * 100).toInt().coerceIn(0, 100)
//            delay(100)
//        }
//    }
//
//    return volumeLevel
//}


actual object VoiceRecorderFactory {
    lateinit var context: Context
    actual fun create(): VoiceRecorder = AndroidVoiceRecorder(context)
}

package com.github.im.group.sdk
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.im.group.manager.VoiceFileManager
import com.github.im.group.sdk.FileData
import io.github.aakira.napier.Napier
import io.github.aakira.napier.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class AndroidVoiceRecorder(
    private val context: Context,
    private val voiceFileManager: VoiceFileManager
) : VoiceRecorder {

    private var outputFile: File? = null
    private var recorder: MediaRecorder? = null
    private var startTime: Long = 0
    private var duration:Long = 0
    private var _isRecording = false
    private var voiceRecordingResult : VoiceRecordingResult? = null

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude


    @RequiresApi(Build.VERSION_CODES.S)
     override fun startRecording() {
        val fileName = "voice_${System.currentTimeMillis()}.m4a"
        val absolutePath = voiceFileManager.getVoiceFileAbsolutePath(fileName)
        outputFile = File(absolutePath)

        recorder = MediaRecorder(context).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }
        startTime = System.currentTimeMillis()
        _isRecording = true
        CoroutineScope(Dispatchers.IO).launch {
            delay(200) // 给 MediaRecorder 启动缓冲时间
            while (_isRecording) {
                try {
                    _amplitude.value = recorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    _amplitude.value = 0 // 捕获异常，避免崩溃
                }
                delay(100)
            }
        }
    }

    override fun getAmplitude(): Int {
        return _amplitude.value
    }
    override fun getOutputFile(): String? = outputFile?.absolutePath

    override fun getVoiceData(): VoiceRecordingResult? {
        // 确保录音已停止 正在录音的时候返回空
        if (_isRecording)
        {
            log { "正在录音中" }
            return null
        }

        return voiceRecordingResult;

    }

    override fun stopRecording(): VoiceRecordingResult? {
        val recorder = recorder ?: return null
        return try {
            recorder.stop()
            recorder.release()
            val bytes = outputFile?.readBytes() ?: return null
            duration = System.currentTimeMillis() - startTime
            val pickedFile = PickedFile(
                name = outputFile!!.name,
                path = outputFile!!.absolutePath,
                mimeType = "audio/mp4",
                size = outputFile!!.length(),
                data = FileData.Bytes(bytes)
            )
            voiceRecordingResult = VoiceRecordingResult(
                bytes = bytes,
                durationMillis = duration,
                pickedFile = pickedFile,
                name = outputFile!!.name,
                filePath = outputFile?.absolutePath
            )
            voiceRecordingResult
        } catch (e: Exception) {
            Napier.d("停止录音失败 ${e.message}")
            null
        } finally {
            this.recorder = null
            _isRecording = false
        }
    }
}


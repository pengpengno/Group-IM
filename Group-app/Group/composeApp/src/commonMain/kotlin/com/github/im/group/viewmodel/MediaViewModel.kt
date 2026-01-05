package com.github.im.group.viewmodel

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.sdk.AudioPlayer
import com.github.im.group.sdk.VoiceRecorder
import com.github.im.group.sdk.VoiceRecordingResult
import io.github.aakira.napier.Napier
import io.github.aakira.napier.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RecorderUiState {
    object Idle : RecorderUiState() // 空闲状态
    object Recording : RecorderUiState() // 正在录音

    /**
     * 录音停止
     */
    object Stop : RecorderUiState()

    object Cancel : RecorderUiState() // 取消

//    object STOP : RecorderUiState() //  停止录音
    data class Playback(
        val filePath: String,
        val duration: Long
    ) : RecorderUiState() // 录音完毕 回放中
}


/**
 * 聊天界面的 ViewModel
 */
class VoiceViewModel(
    private val voiceRecorder: VoiceRecorder,
     val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecorderUiState>(RecorderUiState.Idle)
    val uiState: StateFlow<RecorderUiState> = _uiState

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude

    private var lastFilePath: String? = null
    private var lastDuration: Long = 0


    /***
     * 开始录音
     */
    fun startRecording() {

        viewModelScope.launch(Dispatchers.IO) {
            voiceRecorder.startRecording()
            _uiState.value = RecorderUiState.Recording

            delay(200)
            while (_uiState.value is RecorderUiState.Recording) {
                try {
                    _amplitude.value = voiceRecorder.getAmplitude()
                } catch (e: Exception) {
                    _amplitude.value = 0
                    Napier.d( "录音异常 ${e.stackTrace}")
                    stopRecording()
                }
                delay(100)
            }
        }
    }

    fun getVoicePath():String?{
        return lastFilePath
    }


    /**
     * 停止录音
     *
     */
    fun stopRecording() {
        val result = voiceRecorder.stopRecording()
        if (result != null) {
            lastFilePath = voiceRecorder.getOutputFile()
            lastDuration = result.durationMillis
            _uiState.value = RecorderUiState.Stop
            log { "停止录音 ${_uiState.value}" }

        } else {
            _uiState.value = RecorderUiState.Idle
        }
    }


    /**
     * 重置为初始状态
     */
    fun reset(){
        val result = voiceRecorder.stopRecording()
        _uiState.value = RecorderUiState.Idle
    }

    /**
     * 获取录音数据
     */
    fun getVoiceData(): VoiceRecordingResult? {
        return voiceRecorder.getVoiceData()
    }

    /**
     * 取消录音
     */
    fun cancel() {
        val result = voiceRecorder.stopRecording()
        _uiState.value = RecorderUiState.Cancel
        _uiState.value = RecorderUiState.Idle
    }

}
package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.sdk.VoiceRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecorderState(
    val _isRecording:Boolean =false,
    val amplitude: Int =0

)

/**
 * 聊天界面的 ViewModel
 */
class VoiceViewModel (
    val voiceRecorder: VoiceRecorder,
): ViewModel() {

    private val _uiState = MutableStateFlow( RecorderState())

   val  recorderState: StateFlow<RecorderState> = _uiState



    fun startRecording(conversationId: Long) {
        viewModelScope.launch {
            voiceRecorder.startRecording(conversationId)
        }
        _uiState.update {
            it.copy(
                _isRecording = true
            )
        }
        CoroutineScope(Dispatchers.IO).launch {
            delay(200) // 给MediaRecorder启动缓冲时间

            // 循环更新音量
            while (_uiState.value._isRecording) {
                val amp = voiceRecorder.getAmplitude()
                _uiState.update { it.copy(amplitude = amp) }
                delay(100) // 每100ms更新一次
            }
        }

    }
    fun stopRecording() {
         voiceRecorder.stopRecording()
        _uiState.update {
            it.copy(
                _isRecording = false
            )
        }
        println(voiceRecorder.getOutputFile())

    }




}

package com.github.im.group.sdk



import androidx.compose.runtime.Composable
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

class DesktopFilePicker : FilePicker {
    override suspend fun pickImage(): List<com.github.im.group.sdk.File> = pickFiles("*.png;*.jpg;*.jpeg")

    override suspend fun pickVideo(): List<com.github.im.group.sdk.File> = pickFiles("*.mp4;*.avi;*.mkv")

    override suspend fun pickFile(): List<com.github.im.group.sdk.File> = pickFiles("*.*")

    override suspend fun takePhoto(): com.github.im.group.sdk.File? {
        TODO("Not yet implemented")
    }

    private fun pickFiles(filter: String): List<com.github.im.group.sdk.File> {
        val dialog = FileDialog(Frame(), "Choose File", FileDialog.LOAD)
        dialog.isMultipleMode = true
        dialog.file = filter
        dialog.isVisible = true

        return dialog.files?.map {
            File(
                name = it.name,
                path = it.absolutePath,
                mimeType = guessMimeType(it),
                size = it.length()
            )
        } ?: emptyList()
    }

    private fun guessMimeType(file: File): String? {
        return java.nio.file.Files.probeContentType(file.toPath())
    }
}

actual fun getPlatformFilePicker(): FilePicker = DesktopFilePicker()

@Composable
actual fun CameraPreviewView() {
    // 桌面版暂不实现相机预览功能
}
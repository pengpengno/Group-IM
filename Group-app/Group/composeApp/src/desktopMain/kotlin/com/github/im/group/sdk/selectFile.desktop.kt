package com.github.im.group.sdk

import androidx.compose.ui.awt.ComposeWindow
import javax.swing.JFileChooser

actual suspend fun selectFile(): PlatformFile? {
    val chooser = JFileChooser()
    val result = chooser.showOpenDialog(ComposeWindow())
    return if (result == JFileChooser.APPROVE_OPTION) {
        val file = chooser.selectedFile
        object : PlatformFile {
            override val name: String = file.name
            override val bytes: ByteArray = file.readBytes()
        }
    } else null
}
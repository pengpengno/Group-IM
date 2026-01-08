package com.github.im.group.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.im.group.sdk.FilePicker
import com.github.im.group.sdk.PickedFile
import com.github.im.group.sdk.getPlatformFilePicker

/**
 * 文件选取
 */
@Composable
expect fun PlatformFilePickerPanel(
    onDismiss: () -> Unit,
    onFileSelected: (List<PickedFile>) -> Unit
)


@Composable
fun IconTextButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(12.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(32.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
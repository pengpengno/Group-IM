package com.github.im.group.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.im.group.ui.theme.ThemeTokens

@Composable
fun EmptyChatPlaceholder(isGroup: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF8FAFC),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isGroup) "群聊已创建" else "对话已开启",
                    style = MaterialTheme.typography.titleSmall,
                    color = ThemeTokens.TextMain,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isGroup) "发一条消息，开始团队协作。" else "发一条消息，开始这段对话。",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeTokens.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

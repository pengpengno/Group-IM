package com.github.im.group.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.im.group.model.UserInfo

/**
 * 通用的用户项组件，用于展示用户信息
 */
@Composable
fun UserItem(
    userInfo: UserInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(username = userInfo.username, size = 56)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = userInfo.username,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = userInfo.email,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
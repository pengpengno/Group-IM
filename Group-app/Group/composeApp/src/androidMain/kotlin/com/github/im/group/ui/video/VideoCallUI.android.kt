package com.github.im.group.ui.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.im.group.model.UserInfo

/**
 * Android平台专用的视频通话启动器，包含权限处理
 *
 * @param remoteUser 通话的远程用户信息
 * @param onCallEnded 通话结束时的回调函数
 */
@Composable
fun AndroidVideoCallLauncher(
    remoteUser: UserInfo,
    onCallEnded: () -> Unit = {}
) {
    var hasPermissions by remember { mutableStateOf(false) }
    
    // 首先请求必要的权限
    VideoCallPermissionHelper(
        onAllGranted = {
            // 权限已获得
            hasPermissions = true
        },
        onAnyDenied = {
            // 权限被拒绝，直接结束通话
            onCallEnded()
        }
    )
    
    // 当权限被授予时，显示视频通话界面
    if (hasPermissions) {
        VideoCallLauncher(
            remoteUser = remoteUser,
            onCallEnded = onCallEnded
        )
    }
}
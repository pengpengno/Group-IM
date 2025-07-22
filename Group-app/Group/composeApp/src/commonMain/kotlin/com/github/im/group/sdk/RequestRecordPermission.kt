//package com.github.im.group.sdk
//
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import com.google.accompanist.permissions.ExperimentalPermissionsApi
//import com.google.accompanist.permissions.isGranted
//import com.google.accompanist.permissions.rememberPermissionState
//import com.google.accompanist.permissions.shouldShowRationale
//
//@OptIn(ExperimentalPermissionsApi::class)
//@Composable
//fun RequestRecordPermission(onGranted: () -> Unit) {
//    val permissionState = rememberPermissionState(permission = android.Manifest.permission.RECORD_AUDIO)
//
//    LaunchedEffect(Unit) {
//        if (!permissionState.status.isGranted) {
//            permissionState.launchPermissionRequest()
//        }
//    }
//
//    when {
//        permissionState.status.isGranted -> onGranted()
//        permissionState.status.shouldShowRationale -> {
//            Text("需要麦克风权限进行录音")
//        }
//        else -> {
//            Text("麦克风权限被拒绝，请到设置中开启")
//        }
//    }
//}

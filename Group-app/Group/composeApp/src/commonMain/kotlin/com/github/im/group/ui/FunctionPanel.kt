//package com.github.im.group.ui
//
//import androidx.compose.material.icons.Icons
//
//@Composable
//fun FunctionPanel(
//    onSelectFile: () -> Unit,
//    onTakePhoto: () -> Unit,
//    onRecordAudio: () -> Unit,
//    onDismiss: () -> Unit
//) {
//    Surface(
//        color = Color.White,
//        tonalElevation = 8.dp,
//        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
//        modifier = Modifier
//            .fillMaxWidth()
//            .wrapContentHeight()
//            .padding(8.dp)
//    ) {
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceEvenly
//        ) {
//            IconTextButton(Icons.Default.InsertDriveFile, "文件", onSelectFile)
//            IconTextButton(Icons.Default.PhotoCamera, "拍照", onTakePhoto)
//            IconTextButton(Icons.Default.Mic, "语音", onRecordAudio)
//        }
//    }
//}
//
//@Composable
//fun IconTextButton(icon: ImageVector, text: String, onClick: () -> Unit) {
//    Column(
//        modifier = Modifier
//            .padding(12.dp)
//            .clickable(onClick = onClick),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Icon(icon, contentDescription = text, modifier = Modifier.size(32.dp))
//        Text(text, style = MaterialTheme.typography.bodySmall)
//    }
//}

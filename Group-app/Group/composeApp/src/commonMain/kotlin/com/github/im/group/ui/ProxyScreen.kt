//
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.material3.Button
//import androidx.compose.material3.Card
//import androidx.compose.material3.Checkbox
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.input.PasswordVisualTransformation
//import androidx.compose.ui.unit.dp
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//
//
//@Composable
//fun ProxyScreen() {
//    val viewModel: ProxyViewModel = ViewModel()
//    val loginResult by viewModel.loginResult.collectAsState()
//
//    val context = LocalContext.current
//    var targetHost by remember { mutableStateOf("localhost") }
//    var username by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var isLoading by remember { mutableStateOf(false) }
//
//    // 根据登录结果更新UI状态
//    LaunchedEffect(loginResult) {
//        when (loginResult) {
//            is LoginResult.Success -> isLoading = false
//            is LoginResult.Error -> isLoading = false
//            null -> Unit
//        }
//    }
//
//    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
//        Text("代理设置与测试", style = MaterialTheme.typography.headlineSmall)
//
//        OutlinedTextField(
//            value = targetHost,
//            onValueChange = { targetHost = it },
//            label = { Text("目标Host") },
//            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
//        )
//
//        OutlinedTextField(
//            value = username,
//            onValueChange = { username = it },
//            label = { Text("用户名") },
//            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
//        )
//
//        OutlinedTextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("密码") },
//            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
//            visualTransformation = PasswordVisualTransformation()
//        )
//
//        Button(
//            onClick = {
//                if (username.isBlank() || password.isBlank()) {
//                    Toast.makeText(context, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
//                    return@Button
//                }
//
//                isLoading = true
//                viewModel.proxyLogin(username, password, targetHost)
//            },
//            enabled = !isLoading,
//            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp)
//        ) {
//            if (isLoading) {
//                CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
//            } else {
//                Text("通过代理登录")
//            }
//        }
//
//        when (val result = loginResult) {
//            is LoginResult.Success -> {
//                Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
//                    Column(Modifier.padding(16.dp)) {
//                        Text("登录成功！")
//                        Text("Token: ${result.response.token.take(20)}...")
//                    }
//                }
//            }
//
//            is LoginResult.Error -> {
//                Text(
//                    "请求失败: ${result.message ?: "未知错误"}",
//                    Color.Red,
//                    Modifier.padding(top = 16.dp)
//                )
//            }
//
//            null -> Unit
//        }
//    }
//}
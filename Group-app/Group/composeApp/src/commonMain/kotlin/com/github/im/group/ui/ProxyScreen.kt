
@Composable
fun ProxyScreen() {
    val context = LocalContext.current
    var targetHost by remember { mutableStateOf("localhost") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("代理请求示例", style = MaterialTheme.typography.h5, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = targetHost,
            onValueChange = { targetHost = it },
            label = { Text("目标Host") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            visualTransformation = PasswordVisualTransformation()
        )

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                isLoading = true
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val response = ProxyApi.proxyLogin(username, password, targetHost)
                        result = "登录成功！Token: ${response.token.take(20)}..."
                    } catch (e: Exception) {
                        result = "请求失败: ${e.message ?: "未知错误"}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("通过代理登录")
            }
        }

        if (result.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = result,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.im.group.config.AppEnvironment
import com.github.im.group.config.ConfigManager
import com.github.im.group.config.CustomConfig
import com.github.im.group.config.ProxyConfig
import com.github.im.group.ui.Login
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ProxyScreen(
    navHostController: NavHostController,
    ) {
    val configManager: ConfigManager = koinInject()
    val currentConfig by configManager.currentConfig.collectAsState()
    val currentEnvironment by configManager.currentEnvironment.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var expanded by remember { mutableStateOf(true) }

    var useCustom by remember { mutableStateOf(currentEnvironment == AppEnvironment.CUSTOM) }
    var apiHost by remember { mutableStateOf(currentConfig.apiHost) }
    var apiPort by remember { mutableStateOf(currentConfig.apiPort.toString()) }
    var tcpHost by remember { mutableStateOf(currentConfig.tcpHost) }
    var tcpPort by remember { mutableStateOf(currentConfig.tcpPort.toString()) }
    var useTls by remember { mutableStateOf(currentConfig.useTls) }

    LaunchedEffect(currentConfig, currentEnvironment) {
        useCustom = currentEnvironment == AppEnvironment.CUSTOM
        apiHost = currentConfig.apiHost
        apiPort = currentConfig.apiPort.toString()
        tcpHost = currentConfig.tcpHost
        tcpPort = currentConfig.tcpPort.toString()
        useTls = currentConfig.useTls
    }

    val previewConfig = if (useCustom) {
        CustomConfig(
            apiHost = apiHost,
            tcpHost = tcpHost,
            apiPort = apiPort.toIntOrNull() ?: currentConfig.apiPort,
            tcpPort = tcpPort.toIntOrNull() ?: currentConfig.tcpPort,
            useTls = useTls
        )
    } else {
        currentConfig
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { expanded = !expanded })
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("代理设置", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))

        }

        if (expanded) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = useCustom,
                        onCheckedChange = { useCustom = it }
                    )
                    Text("启用代理")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiHost,
                    onValueChange = { apiHost = it },
                    label = { Text("代理Host") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = apiPort,
                    onValueChange = { apiPort = it.filter(Char::isDigit) },
                    label = { Text("代理端口") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = tcpHost,
                    onValueChange = { tcpHost = it },
                    label = { Text("TCP Host") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tcpPort,
                    onValueChange = { tcpPort = it.filter(Char::isDigit) },
                    label = { Text("TCP Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = useTls,
                        onCheckedChange = { useTls = it }
                    )
                    Text("Use TLS")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "当前代理地址: ${ProxyConfig.getBaseUrl()}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (useCustom) {
                                configManager.updateCustomConfig(
                                    host = apiHost,
                                    tcpHost = tcpHost,
                                    port = apiPort.toIntOrNull() ?: currentConfig.apiPort,
                                    tcpPort = tcpPort.toIntOrNull() ?: currentConfig.tcpPort,
                                    useTls = useTls
                                )
                            } else {
                                configManager.setEnvironment(AppEnvironment.PROD)
                            }
                            navHostController.navigate(Login)
                        }
//                        navigator.push(LoginScreen())
                    },
                ){
                    Text("保存")
                }

            }
        }
    }
}

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ProxySettingItem() {
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = FocusRequester()

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
            Icon(
                contentDescription = if (expanded) "收起" else "展开"
            )
        }

        if (expanded) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = ProxyConfig.enableProxy,
                        onCheckedChange = { ProxyConfig.enableProxy = it }
                    )
                    Text("启用代理")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ProxyConfig.host,
                    onValueChange = { ProxyConfig.host = it },
                    label = { Text("代理Host") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = ProxyConfig.port.toString(),
                    onValueChange = {
                        if (it.isEmpty()) {
                            ProxyConfig.port = 0
                        } else {
                            val port = it.toIntOrNull()
                            if (port != null && port in 1..65535) {
                                ProxyConfig.port = port
                            }
                        }
                    },
                    label = { Text("代理端口") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "当前代理地址: ${ProxyConfig.getBaseUrl()}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
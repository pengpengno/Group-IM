import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ProxyConfig {
    var host by mutableStateOf("192.168.1.6")
    var port by mutableStateOf(8080)
    var tcp_port by mutableStateOf(8088)
    var enableProxy by mutableStateOf(false)

    fun getBaseUrl(): String {
        return if (enableProxy) {
            "http://$host:$port"
        } else {
            "http://$host:8080"
        }
    }
}
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ProxyConfig {
    var host by mutableStateOf("localhost")
    var port by mutableStateOf(8080)
    var enableProxy by mutableStateOf(false)

    fun getBaseUrl(): String {
        return if (enableProxy) {
            "http://$host:$port"
        } else {
            "http://localhost:8080"
        }
    }
}
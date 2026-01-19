
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ProxyConfig {
//    var host by mutableStateOf("192.168.4.25")
//    var host by mutableStateOf("192.168.71.57")
    var host by mutableStateOf("10.122.155.252")
//    var host by mutableStateOf("192.168.244.252")
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

//
///**
// * 代理设置
// */
//public data class ProxySettingsState(
//    val host: String = "192.168.1.6",
//    val port: Int = 8080,
//    val tcpPort: Int = 8088,
//    val enableProxy: Boolean = false
//) {
//    fun getBaseUrl(): String {
//        return if (enableProxy) {
//            "http://$host:$port"
//        } else {
//            "http://$host:8080"
//        }
//    }
//}
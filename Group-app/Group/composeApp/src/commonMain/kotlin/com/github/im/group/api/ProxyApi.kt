
import com.github.im.group.GlobalCredentialProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent


object ProxyApi  : KoinComponent {

    val client = HttpClient() {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    // 基础请求方法，使用全局代理配置
    suspend inline fun <reified T> request(
        hmethod: HttpMethod,
        path: String,
        body: Any? = null,
        block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        val baseUrl = if (ProxyConfig.enableProxy) {
            "http://${ProxyConfig.host}:${ProxyConfig.port}"
        } else {
            "http://192.168.2.2:8080"
        }



        return client.request("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            method = hmethod
            if (body != null) {
                setBody(body)
            }
            val token = GlobalCredentialProvider.storage.getUserInfo()?.token

            if( token != null){

                header("Proxy-Authorization", "Basic ${token}")
                header("Authorization", "Bearer ${token}")
            }

            block()
        }.body()
    }

}
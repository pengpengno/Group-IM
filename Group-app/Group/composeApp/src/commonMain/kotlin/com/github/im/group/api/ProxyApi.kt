
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json



object ProxyApi {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    // 基础请求方法，使用全局代理配置
    suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        block: HttpRequestBuilder.() -> Unit = {}
    ): T {
        val baseUrl = if (ProxyConfig.enableProxy) {
            "http://${ProxyConfig.host}:${ProxyConfig.port}"
        } else {
            "http://localhost:8080"
        }

        return client.request("$baseUrl$path") {
//            method = method
            contentType(ContentType.Application.Json)
            if (body != null) {
                setBody(body)
            }
            block()
        }.body()
    }


}
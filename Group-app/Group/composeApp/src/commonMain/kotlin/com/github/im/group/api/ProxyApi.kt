
import com.github.im.group.GlobalCredentialProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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
//    suspend inline fun <reified T> request(
    suspend inline fun <reified R : Any, reified B : Any?> request(
        hmethod: HttpMethod,
        path: String,
        body: B? = null,
        requestParams : Map<String,String>?=null,
        block: HttpRequestBuilder.() -> Unit = {}
    ): R {
        val baseUrl = if (ProxyConfig.enableProxy) {
            "http://${ProxyConfig.host}:${ProxyConfig.port}"
        } else {
            "http://192.168.1.14:8080"
        }

        val response = client.request("$baseUrl$path") {
            url {
                if(!requestParams.isNullOrEmpty()){
                    requestParams.forEach { (key, value) ->
                        parameters.append(key, value)
                    }
                }
            }
            contentType(ContentType.Application.Json)
            method = hmethod
            if (body != null) {
                setBody(body)
            }


            val token = GlobalCredentialProvider.storage.getUserInfo()?.token
            if (token != null) {
                header("Proxy-Authorization", "Basic $token")
                header("Authorization", "Bearer $token")
            }

            block()
        }

        // ✅ 统一判断状态码
        return when (response.status.value) {
            in 200..299 -> {
                response.body()
            }
            in 400..499 -> {
                val errorText = response.bodyAsText()
                println("ProxyApi error: $response")

//                Logger .d("ProxyApi", "ProxyApi error: $errorText")
                throw RuntimeException("客户端请求错误：${response.status}，内容: $errorText")
            }
            in 500..599 -> {
                val errorText = response.bodyAsText()
                throw RuntimeException("服务器错误：${response.status}，内容: $errorText")
            }
            else -> {
                val errorText = response.bodyAsText()
                throw RuntimeException("未知响应状态：${response.status}，内容: $errorText")
            }
        }
    }

}
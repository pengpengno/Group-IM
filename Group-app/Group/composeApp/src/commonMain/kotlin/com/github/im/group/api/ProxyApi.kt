import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.api.FileUploadResponse
import com.github.im.group.manager.LoginStateManager
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


object ProxyApi
  : KoinComponent {

    val client = HttpClient() {
        install(ContentNegotiation) {
            val token = GlobalCredentialProvider.currentToken

            headersOf("Authorization",  "Bearer $token")
            json(Json {
                classDiscriminator = "type"  // 指定多态字段名
                ignoreUnknownKeys = true
                coerceInputValues = true     // null 值使用默认
            })
        }
    }


    /**
     * 文件上传
     * @param file  文件
     * @param fileName 文件名
     * @param duration 文件时长 适用于音频/ 视频文件
     */
    suspend fun uploadFile(fileId:String ,file: ByteArray, fileName: String , duration: Long=0):FileUploadResponse {

        val baseUrl = if (ProxyConfig.enableProxy) {
            "http://${ProxyConfig.host}:${ProxyConfig.port}"
        } else {
            "http://${ProxyConfig.host}:${ProxyConfig.port}"
        }


        val response =  client.submitFormWithBinaryData(
            url = baseUrl + "/api/files/upload",
            formData = formData {

                append("uploaderId", fileId)
                if (duration > 0){
                    append("duration", duration)
                }
                append("file", file, Headers.build {
                append(HttpHeaders.ContentDisposition, "filename*=UTF-8''${java.net.URLEncoder.encode(fileName, "UTF-8")}")
                })
            },

        ){
            method = HttpMethod.Post
            headers {
                val token = GlobalCredentialProvider.currentToken
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                    header("Proxy-Authorization", "Bearer $token")
                }
            }

        }

        Napier.d("ProxyApi Upload response: ${response.bodyAsText()}")
        return when (response.status.value) {
            in 200..299 -> {
                val responseBody = response.body<FileUploadResponse>()
                return responseBody
            }

            401 -> {
                val errorText = response.bodyAsText()
                throw UnauthorizedException("认证失败，请重新登录：${response.status}，内容: $errorText")
            }
            in 400..499 -> {
                val errorText = response.bodyAsText()
//                logger("ProxyApi", "ProxyApi error: $errorText")
                throw ClientRequestException("客户端请求错误：${response.status}，内容: $errorText")
            }
            in 500..599 -> {
                val errorText = response.bodyAsText()
                throw ServerException("服务器错误：${response.status}，内容: $errorText")
            }
            else -> {
                val errorText = response.bodyAsText()
                throw UnknownResponseException("未知响应状态：${response.status}，内容: $errorText")
            }
        }
    }
    
    /**
     * 文件上传（带客户端ID）
     * @param file  文件
     * @param fileName 文件名
     * @param duration 文件时长 适用于音频/ 视频文件
     * @param clientId 客户端生成的UUID
     */
    suspend fun uploadFileWithClientId(file: ByteArray, fileName: String, duration: Long, clientId: String): FileUploadResponse {

        val baseUrl = if (ProxyConfig.enableProxy) {
            "http://${ProxyConfig.host}:${ProxyConfig.port}"
        } else {
            "http://${ProxyConfig.host}:${ProxyConfig.port}"
        }


        var response =  client.submitFormWithBinaryData(
            url = baseUrl + "/api/files/upload",
            formData = formData {

                append("uploaderId", clientId)
                if (duration > 0){
                    append("duration", duration)
                }
                append("file", file, Headers.build {
                append(HttpHeaders.ContentDisposition, "filename*=UTF-8''${java.net.URLEncoder.encode(fileName, "UTF-8")}")
                })
            },

        ){
            method = HttpMethod.Post
            headers {
                val token = GlobalCredentialProvider.currentToken
                if (token.isNotEmpty()) {
                    header("Authorization", "Bearer $token")
                    header("Proxy-Authorization", "Bearer $token")
                }
            }

        }
        return when (response.status.value) {
            in 200..299 -> {
                val responseBody = response.body<FileUploadResponse>()
                return responseBody
            }

            401 -> {
                val errorText = response.bodyAsText()
                throw UnauthorizedException("认证失败，请重新登录：${response.status}，内容: $errorText")
            }
            in 400..499 -> {
                val errorText = response.bodyAsText()
//                logger("ProxyApi", "ProxyApi error: $errorText")
                throw ClientRequestException("客户端请求错误：${response.status}，内容: $errorText")
            }
            in 500..599 -> {
                val errorText = response.bodyAsText()
                throw ServerException("服务器错误：${response.status}，内容: $errorText")
            }
            else -> {
                val errorText = response.bodyAsText()
                throw UnknownResponseException("未知响应状态：${response.status}，内容: $errorText")
            }
        }
    }
    // 基础请求方法，使用全局代理配置
//    suspend inline fun <reified T> request(
    suspend inline fun <reified B : Any, reified R : Any?> request(
        hmethod: HttpMethod,
        path: String,
        body: B? = null,
        requestParams : Map<String,String>?=null,
        headers : Map<String,Any>?=null,
        block: HttpRequestBuilder.() -> Unit = {}
    ): R {
//        val config = proxyConfigProvider()

        val baseUrl = if (ProxyConfig.enableProxy) {
            "http://${ProxyConfig.host}:${ProxyConfig.port}"
        } else {
            "http://${ProxyConfig.host}:${ProxyConfig.port}"
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

//            val token = GlobalCredentialProvider.storage.getUserInfo()?.token
            val token = GlobalCredentialProvider.currentToken
            if (headers != null) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            if (token.isNotEmpty()) {
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
            401 -> {
                val errorText = response.bodyAsText()
                throw UnauthorizedException("认证失败，请重新登录：${response.status}，内容: $errorText")
            }
            in 400..499 -> {
                val errorText = response.bodyAsText()
                Napier.d("ProxyApi error: $response, paramter= {$body} " )

//                Logger .d("ProxyApi", "ProxyApi error: $errorText")
                throw ClientRequestException("客户端请求错误：${response.status}，内容: $errorText")
            }
            in 500..599 -> {
                val errorText = response.bodyAsText()
                throw ServerException("服务器错误：${response.status}，内容: $errorText")
            }
            else -> {
                val errorText = response.bodyAsText()
                throw UnknownResponseException("未知响应状态：${response.status}，内容: $errorText")
            }
        }
    }

}

// 自定义异常类
class UnauthorizedException(message: String) : Exception(message)
class ClientRequestException(message: String) : Exception(message)
class ServerException(message: String) : Exception(message)
class UnknownResponseException(message: String) : Exception(message)
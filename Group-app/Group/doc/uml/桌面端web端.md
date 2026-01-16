目标说明（给 AI 的上下文）

构建一个 IM 系统客户端，同时支持

Android（Compose）

桌面 + Web（Electron + Chromium）

核心业务逻辑使用 Kotlin Multiplatform（KMP）复用

系统包含：

登录

IM 长连接（TCP / WebSocket）

WebRTC 音视频

会话状态管理

要求：

Electron 用于桌面和 Web

WebRTC 使用 Chromium 原生能力

KMP 只负责业务逻辑，不写 UI

一、整体实施阶段拆解（必须按顺序）
Phase 0：基础工程准备（不要跳）
Step 0.1 创建 KMP 项目

使用 Kotlin Multiplatform

必须包含：

commonMain

androidMain

jsMain

📌 不要引入 UI 框架到 commonMain

Step 0.2 commonMain 依赖约定
dependencies {
implementation("io.ktor:ktor-client-core")
implementation("io.ktor:ktor-client-content-negotiation")
implementation("io.ktor:ktor-serialization-kotlinx-json")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
}

Phase 1：commonMain（核心复用层）
Step 1.1 定义基础模型（DTO）
@Serializable
data class User(
val id: String,
val nickname: String
)

@Serializable
data class Message(
val id: String,
val from: String,
val content: String,
val timestamp: Long
)

Step 1.2 登录与 Token 管理
class AuthRepository(private val client: HttpClient) {

    suspend fun login(username: String, password: String): String {
        return client.post("/login") {
            setBody(mapOf(
                "username" to username,
                "password" to password
            ))
        }.body<String>()
    }
}

Step 1.3 IM 会话状态机（重点）
sealed class ImState {
object Disconnected : ImState()
object Connecting : ImState()
object Connected : ImState()
data class Error(val reason: String) : ImState()
}

class ImSession {

    private val _state = MutableStateFlow<ImState>(ImState.Disconnected)
    val state: StateFlow<ImState> = _state

    suspend fun connect() {
        _state.value = ImState.Connecting
        // 建立 TCP / WS 连接
        _state.value = ImState.Connected
    }

    fun disconnect() {
        _state.value = ImState.Disconnected
    }
}

Step 1.4 消息收发抽象
interface MessageTransport {
suspend fun send(message: Message)
fun observe(onMessage: (Message) -> Unit)
}

Phase 2：JS 导出（给 Electron 用）
Step 2.1 Facade 层（唯一对外入口）
@JsExport
class ImFacade {

    private val session = ImSession()

    fun connect() {
        GlobalScope.launch {
            session.connect()
        }
    }

    fun observeState(callback: (String) -> Unit) {
        GlobalScope.launch {
            session.state.collect {
                callback(it::class.simpleName ?: "Unknown")
            }
        }
    }
}


📌 AI 必须遵守：

只通过 Facade 暴露能力

不允许直接暴露 internal Flow / Coroutine

Step 2.2 KMP 编译为 JS
kotlin {
js(IR) {
browser()
binaries.executable()
}
}


输出：

shared.js
shared.d.ts

Phase 3：Electron 工程
Step 3.1 初始化 Electron
npm init -y
npm install electron

Step 3.2 主进程（main.js）
const { app, BrowserWindow } = require("electron")

function createWindow() {
const win = new BrowserWindow({
width: 1200,
height: 800,
webPreferences: {
nodeIntegration: true
}
})

win.loadURL("http://localhost:3000")
}

app.whenReady().then(createWindow)

Step 3.3 Renderer 端（UI）
import { ImFacade } from "./shared.js"

const im = new ImFacade()

im.observeState(state => {
console.log("IM State:", state)
})

im.connect()

Phase 4：WebRTC（Electron / Chromium）
Step 4.1 摄像头与麦克风
const stream = await navigator.mediaDevices.getUserMedia({
video: true,
audio: true
})

videoElement.srcObject = stream

Step 4.2 WebRTC 信令（职责划分）

📌 AI 必须遵守以下边界：

模块	职责
KMP	状态管理、业务规则
Electron	WebRTC API
Server	信令转发
UI	展示
Phase 5：Android 客户端（复用）
Step 5.1 Android 调用 KMP
val im = ImSession()

LaunchedEffect(Unit) {
im.state.collect {
println(it)
}
}

Phase 6：约束规则（非常重要，直接给 AI）

必须遵守以下约束：

❌ commonMain 不允许任何 UI 代码

❌ commonMain 不使用 Android / DOM API

✅ WebRTC 只在 Electron / Browser

✅ IM 状态机必须在 commonMain

✅ 所有跨端能力通过 Facade 暴露

Phase 7：输出要求（给 AI 的指令）

在生成代码时：

所有代码必须是 可编译 / 可运行

禁止伪代码

每个模块提供最小可运行示例

优先 Kotlin / JavaScript

不使用实验性 API

官方参考来源（你要求的）

Kotlin Multiplatform
https://kotlinlang.org/docs/multiplatform.html

Kotlin JS Export
https://kotlinlang.org/docs/js-to-kotlin-interop.html#jsexport-annotation

Electron WebRTC
https://www.electronjs.org/docs/latest/tutorial/webrtc

Ktor Client
https://ktor.io/docs/client.html

下一步你可以直接让我做的事

你可以直接说其中一句：

“把 Phase 1 生成成完整项目代码”

“补齐 WebRTC 信令 + 状态机”

“生成 IM 客户端协议设计”

“给我一个 AI 可直接执行的 System Prompt 版本”

我会按工程可落地标准继续。
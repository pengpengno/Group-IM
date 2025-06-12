package com.github.im.group.sdk

import com.github.im.group.config.SocketClient
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.model.proto.PlatformType

class SenderSdk(
//    private val tcpClient: TcpClient,
    private val tcpClient: SocketClient,
) {

    /***
     * 用于 向远程长连接服务器建立连接
     */
    suspend fun loginConnect(userInfo: UserInfo) {
        tcpClient.connect("192.168.1.14",8088)

        val accountInfo = AccountInfo(
            account = userInfo.username,
            accountName = userInfo.username,
            userId = userInfo.userId,
            eMail = userInfo.email,
            platformType = PlatformType.ANDROID,

        )

        val data = AccountInfo.ADAPTER.encode(accountInfo)
        tcpClient.send(data)
    }

}
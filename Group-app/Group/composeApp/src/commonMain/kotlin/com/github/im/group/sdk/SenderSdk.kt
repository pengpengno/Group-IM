package com.github.im.group.sdk

import ProxyConfig
import com.github.im.group.config.SocketClient
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.model.proto.BaseMessagePkg
import com.github.im.group.model.proto.PlatformType

class SenderSdk(
    private val tcpClient: SocketClient,
) {

    /***
     * 用于 向远程长连接服务器建立连接
     */
    suspend fun loginConnect(userInfo: UserInfo) {
        tcpClient.connect(ProxyConfig.host,8088)

        val accountInfo = AccountInfo(
            account = userInfo.username,
            accountName = userInfo.username,
            userId = userInfo.userId,
            eMail = userInfo.email,
            platformType = PlatformType.ANDROID,

        )

        var message = BaseMessagePkg(
            accountInfo = accountInfo
        ).let {
            BaseMessagePkg.ADAPTER.encode(it)
        }

//        val data = AccountInfo.ADAPTER.encode(accountInfo)
        tcpClient.send(message)
    }

}
package com.github.im.group

//全局 凭据存储实例
// commonMain
object GlobalCredentialProvider {
    var storage: CredentialStorage = DefaultCredentialStorage


   lateinit var proxySettingsState : ProxyConfigStorage


    var currentToken :String = ""
    var currentUserId: Long = 0L
    var companyId: Long? = null



}
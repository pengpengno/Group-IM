package com.github.im.group

//全局 凭据存储实例
// commonMain
object GlobalCredentialProvider {
    var storage: CredentialStorage = DefaultCredentialStorage
    var currentToken :String = ""


}

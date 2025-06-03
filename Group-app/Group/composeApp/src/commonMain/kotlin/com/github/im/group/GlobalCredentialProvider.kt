package com.github.im.group

//全局 凭据存储实例
// commonMain
object GlobalCredentialProvider {
    var storage: CredentialStorage = DefaultCredentialStorage
    fun setStorage(custom: CredentialStorage) {
        storage = custom
    }

    fun getStorage(): CredentialStorage = storage
}

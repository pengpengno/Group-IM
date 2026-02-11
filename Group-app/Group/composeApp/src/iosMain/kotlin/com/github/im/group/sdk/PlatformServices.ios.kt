package com.github.im.group.sdk

/**
 * iOS平台服务工厂实现
 */
class IosPlatformServices : PlatformServices {
    private val dataStorage: DataStorage by lazy { IosDataStorage() }
    private val fileStorage: FileStorage by lazy { IosFileStorage() }
    private val databaseManager: DatabaseManager by lazy { IosDatabaseManager() }
    
    override fun getWebRTCManager(): WebRTCManager = IosWebRTCManager()
    override fun getVoiceRecorder(): VoiceRecorder = IosVoiceRecorder()
    override fun getFilePicker(): FilePicker = getPlatformFilePicker()
    override fun getAudioPlayer(): AudioPlayer = IosAudioPlayer()
    override fun getNetworkManager(): NetworkManager = IosNetworkManager()
    override fun getDatabaseManager(): DatabaseManager = databaseManager
    override fun getDataStorage(): DataStorage = dataStorage
    override fun getFileStorage(): FileStorage = fileStorage
    override fun getUserManager(): UserManager = IosUserManager(dataStorage)
}

/**
 * 获取iOS平台服务工厂
 */
fun getIosPlatformServices(fileStorageManager: FileStorageManager? = null): PlatformServices = IosPlatformServices(fileStorageManager)

actual fun getPlatformServices(): PlatformServices = getIosPlatformServices()
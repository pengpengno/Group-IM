package com.github.im.group

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build

class AndroidPlatform() : Platform {
//    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val name: String = "ANDROID"

    public lateinit var context : Context
//    lateinit val context : Context? = null

    fun getAndroidContext(): Context {
        return context ?: throw Exception("Android context not initialized")
    }
    fun setAndroidContext(context: Context) {
        this.context = context
    }

}

@SuppressLint("StaticFieldLeak")
lateinit var androidContext: Context

/**
 * 初始化 安卓 上下文
 */
fun initAndroidContext(ctx: Context) {
    androidContext = ctx
}
actual fun getPlatform(): Platform = AndroidPlatform()
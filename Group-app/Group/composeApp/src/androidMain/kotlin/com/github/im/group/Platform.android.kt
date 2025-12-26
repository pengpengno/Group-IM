package com.github.im.group

import android.os.Build

class AndroidPlatform : Platform {
//    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val name: String = "ANDROID"
}

actual fun getPlatform(): Platform = AndroidPlatform()
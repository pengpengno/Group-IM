package com.github.im.group

import cafe.adriel.voyager.core.registry.ScreenProvider

sealed class SharedScreen:ScreenProvider {

    object Post :SharedScreen()
    data class  PostDetails (val id:String) :SharedScreen()


}

package com.github.im.group.config

import com.github.im.group.Greeting
import com.github.im.group.viewmodel.UserViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// commonMain/kotlin/com/github/im/group/di/AppModule.kt
val appModule = module {
    single { Greeting() }
    viewModelOf(::UserViewModel) // 注册为 ViewModel，由 Koin 自动管理生命周期
}

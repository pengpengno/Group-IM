package com.github.im.group

import com.github.im.group.api.LoginApi
import com.github.im.group.api.SpaceXApi
import com.github.im.group.db.SpaceXSDK
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single<SpaceXApi> { SpaceXApi() }
    single<SpaceXSDK> {
        SpaceXSDK(
            databaseDriverFactory = AndroidDatabaseDriverFactory(
                androidContext()
            ), api = get()
        )
    }

    single<LoginApi> { LoginApi }
}
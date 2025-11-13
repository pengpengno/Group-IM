package com.github.im.group

import com.github.im.group.api.LoginApi
import com.github.im.group.db.DatabaseDriverFactory
import com.github.im.group.repository.FilesRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.FileStorageManager
import com.github.im.group.viewmodel.UserViewModel
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.dsl.module

val commonModule = module {

}
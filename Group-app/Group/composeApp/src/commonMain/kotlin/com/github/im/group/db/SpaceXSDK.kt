package com.github.im.group.db

import com.github.im.group.api.SpaceXApi


class SpaceXSDK(databaseDriverFactory: DatabaseDriverFactory, val api: SpaceXApi) {
    private val database = Database(databaseDriverFactory)
}
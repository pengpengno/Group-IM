package com.github.im.group.repository

import com.github.im.group.db.AppDatabase
import com.github.im.group.model.DepartmentInfo
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

/**
 * 组织架构离线缓存仓库
 * 负责将组织架构数据序列化存储到本地 SQLite，实现离线可用
 */
class OrganizationRepository(
    private val db: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * 将组织架构数据保存到本地缓存
     */
    fun saveOrgStructure(departments: List<DepartmentInfo>) {
        try {
            val encoded = json.encodeToString(departments)
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            db.organizationQueries.insertOrUpdateCache(
                departmentsJson = encoded,
                updated_at = now
            )
            Napier.d("组织架构数据已缓存到本地，共 ${departments.size} 个部门")
        } catch (e: Exception) {
            Napier.e("保存组织架构缓存失败", e)
        }
    }

    /**
     * 从本地缓存读取组织架构数据
     * 弱网或无网时使用
     */
    fun loadCachedOrgStructure(): List<DepartmentInfo>? {
        return try {
            val cached = db.organizationQueries.getCache().executeAsOneOrNull()
                ?: return null
            val departments = json.decodeFromString<List<DepartmentInfo>>(cached.departmentsJson)
            Napier.d("从本地缓存加载组织架构，共 ${departments.size} 个部门，缓存时间: ${cached.updated_at}")
            departments
        } catch (e: Exception) {
            Napier.e("读取组织架构缓存失败", e)
            null
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        try {
            db.organizationQueries.insertOrUpdateCache(
                departmentsJson = "[]",
                updated_at = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )
        } catch (e: Exception) {
            Napier.e("清除组织架构缓存失败", e)
        }
    }
}

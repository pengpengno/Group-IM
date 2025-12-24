package com.github.im.group.model

import kotlinx.serialization.Serializable

/**
 * 通用的API响应结构
 */
@Serializable
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null,
    val timestamp: Long = 0
)

/**
 * 组织架构模型
 */
typealias OrganizationStructure = ApiResponse<DepartmentInfo>

@Serializable
data class DepartmentInfo(
    val departmentId: Long = 0,
    val name: String = "",
    val description: String? = null,
    val companyId: Long = 0,
    val parentId: Long? = null,
    val status: Boolean = true,
    val children: List<DepartmentInfo> = emptyList(),
    val members: List<UserInfo> = emptyList() // 部门下的成员
)

// 用于UI展示的组织架构节点数据类
data class OrgTreeNode(
    val id: Long,
    val name: String,
    val type: NodeType,
    val parentId: Long? = null,
    val children: List<OrgTreeNode> = emptyList(),
    val userInfo: UserInfo? = null, // 如果是用户节点，则包含用户信息
    val departmentInfo: DepartmentInfo? = null // 如果是部门节点，则包含部门信息
) {
    enum class NodeType {
        DEPARTMENT, USER
    }
}
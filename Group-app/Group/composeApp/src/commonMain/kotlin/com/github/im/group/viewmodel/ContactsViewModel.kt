package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.api.OrganizationApi
import com.github.im.group.api.UserApi
import com.github.im.group.model.DepartmentInfo
import com.github.im.group.model.OrgTreeNode
import com.github.im.group.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(
    val userRepository: UserRepository,
) : ViewModel() {

    private val _organizationStructureState = MutableStateFlow<DepartmentInfo?>(null)
    private val _organizationTreeState = MutableStateFlow<List<OrgTreeNode>>(emptyList())

    val organizationStructure: StateFlow<DepartmentInfo?> = _organizationStructureState.asStateFlow()
    val organizationTree: StateFlow<List<OrgTreeNode>> = _organizationTreeState.asStateFlow()

    private val _loading = MutableStateFlow(false)

    val loading: StateFlow<Boolean> = _loading

    private val _expandedDepartments = MutableStateFlow<Set<Long>>(emptySet())

    val expandedDepartments: StateFlow<Set<Long>> = _expandedDepartments.asStateFlow()

    /**
     * 查询用户
     */
    suspend fun queryUser(queryString: String) {
        viewModelScope.launch {
            // 本地用户搜索 远程用户搜索 、 本地信息搜索（所有信息都应该在本地存储）
            UserApi.findUser(queryString)
        }
    }

    /**
     * 获取组织架构
     */
    suspend fun loadOrganizationStructure() {
        _loading.value = true
        try {
            val structure = OrganizationApi.getCurrentUserOrganizationStructure()
            _organizationStructureState.value = structure.data
            // 同时构建树形结构用于UI展示
            _organizationTreeState.value = if (structure.data != null) {
                buildOrgTree(listOf(structure.data))
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 可以添加错误处理
        } finally {
            _loading.value = false
        }
    }

    /**
     * 构建组织树结构用于UI展示
     */
    private fun buildOrgTree(departments: List<DepartmentInfo>): List<OrgTreeNode> {
        return departments.map { department ->
            OrgTreeNode(
                id = department.departmentId,
                name = department.name,
                type = OrgTreeNode.NodeType.DEPARTMENT,
                parentId = department.parentId,
                children = buildOrgTreeForDepartment(department),
                departmentInfo = department
            )
        }
    }

    private fun buildOrgTreeForDepartment(department: DepartmentInfo): List<OrgTreeNode> {
        val childrenNodes = department.children.map { childDept ->
            OrgTreeNode(
                id = childDept.departmentId,
                name = childDept.name,
                type = OrgTreeNode.NodeType.DEPARTMENT,
                parentId = childDept.parentId,
                children = buildOrgTreeForDepartment(childDept),
                departmentInfo = childDept
            )
        }

        val userNodes = department.members.map { user ->
            OrgTreeNode(
                id = user.userId,
                name = user.username,
                type = OrgTreeNode.NodeType.USER,
                parentId = department.departmentId,
                children = emptyList(),
                userInfo = user
            )
        }

        return (childrenNodes + userNodes).sortedBy { it.name }
    }

    /**
     * 切换部门展开/折叠状态
     */
    fun toggleDepartmentExpanded(departmentId: Long) {
        val currentSet = _expandedDepartments.value.toMutableSet()
        if (currentSet.contains(departmentId)) {
            currentSet.remove(departmentId)
        } else {
            currentSet.add(departmentId)
        }
        _expandedDepartments.value = currentSet
    }

    /**
     * 获取当前用户的组织架构
     */
    fun getOrganizationStructure() {
        viewModelScope.launch {
            loadOrganizationStructure()
        }
    }

    /**
     * 获取展开的部门ID集合
     */
    fun getExpandedDepartments(): Set<Long> {
        return _expandedDepartments.value
    }

    /**
     * 展开指定部门
     */
    fun expandDepartment(departmentId: Long) {
        val currentSet = _expandedDepartments.value.toMutableSet()
        currentSet.add(departmentId)
        _expandedDepartments.value = currentSet
    }

    /**
     * 折叠指定部门
     */
    fun collapseDepartment(departmentId: Long) {
        val currentSet = _expandedDepartments.value.toMutableSet()
        currentSet.remove(departmentId)
        _expandedDepartments.value = currentSet
    }
}
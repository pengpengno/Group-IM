package com.github.im.group.ui.workbench.notification

import com.github.im.group.api.WorkbenchApi
import com.github.im.group.api.WorkbenchTaskDetail
import com.github.im.group.viewmodel.LoginState
import com.github.im.group.viewmodel.UserViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

sealed interface WorkbenchNavigationResult {
    data class TaskLoaded(val detail: WorkbenchTaskDetail) : WorkbenchNavigationResult
    data class Unsupported(val message: String) : WorkbenchNavigationResult
    data class Failed(val message: String) : WorkbenchNavigationResult
}

suspend fun navigateWorkbenchTarget(
    userViewModel: UserViewModel,
    target: WorkbenchDeepLinkTarget,
): WorkbenchNavigationResult {
    val current = userViewModel.currentLocalUserInfo.value
        ?: return WorkbenchNavigationResult.Failed("登录状态已失效，请重新登录")

    if (current.currentLoginCompanyId != target.companyId) {
        val hasMembership = current.companies.any { it.companyId == target.companyId }
        if (!hasMembership) {
            return WorkbenchNavigationResult.Failed("你已不在该工作区，无法打开此工作台资源")
        }

        userViewModel.switchWorkspace(target.companyId)
        val switched = withTimeoutOrNull(15_000L) {
            userViewModel.loginState.first { state ->
                state is LoginState.Authenticated && state.userInfo.currentLoginCompanyId == target.companyId
            }
            true
        } ?: false
        if (!switched) {
            return WorkbenchNavigationResult.Failed("工作区切换失败，请稍后重试")
        }
    }

    return when (target.category) {
        WorkbenchCardCategory.TASK -> {
            val taskId = target.resourceId.toLongOrNull()
                ?: return WorkbenchNavigationResult.Failed("任务链接无效")
            runCatching { WorkbenchApi.taskDetail(taskId) }
                .fold(
                    onSuccess = { WorkbenchNavigationResult.TaskLoaded(it) },
                    onFailure = {
                        WorkbenchNavigationResult.Failed(
                            it.message?.takeIf(String::isNotBlank) ?: "任务不存在或你无权访问",
                        )
                    },
                )
        }

        WorkbenchCardCategory.APPROVAL,
        WorkbenchCardCategory.ANNOUNCEMENT,
        WorkbenchCardCategory.SCHEDULE,
        WorkbenchCardCategory.REPORT -> WorkbenchNavigationResult.Unsupported("该工作台类型暂未在移动端开放详情")
    }
}

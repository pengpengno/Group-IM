package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.api.LoginApi
import com.github.im.group.api.PageResult
import com.github.im.group.api.UserApi
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.SenderSdk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 *  联系人 ViewModel
 *  会话 相关 Api  也在此
 */
class ContactsViewModel(
    private val senderSdk: SenderSdk,
    val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<UserInfo>>(emptyList())

    val uiState:  StateFlow<List<UserInfo>> = _uiState.asStateFlow()



    /**
     *  查询所有的联系人
     *
     *  1.  在第一次上线的时候，通过API 查询所有的联系人关系 Id 与本地进行比对 ，如有异同 以服务端为准， 并且更新本地数据库
     *
     *  a) 所有的联系人信息都通过MutableStateFlow 存储起来
     *  b) 这些所有数据都要存储到数据库中
     *  2  上线成功后通过 TCP 长链接来 实时更新 联系人
     *  3. 本地和线上有差异的数据 通过API 获取更多详细的信息 ，比对的时候只 对 两端 Id 先排序，再 逐个比较 ，判断当前联系人关系两端是否一直
     *
     */
    fun getAllContacts(): List<UserInfo> {
        return emptyList()

//        return userRepository.getAllContacts()
    }


}

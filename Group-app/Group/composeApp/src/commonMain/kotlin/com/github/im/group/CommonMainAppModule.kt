package com.github.im.group

import com.github.im.group.repository.ConversationRepository
import com.github.im.group.repository.UserRepository
import com.github.im.group.service.SessionPreCreationService
import com.github.im.group.service.SessionPreCreationServiceImpl
import com.github.im.group.viewmodel.ContactsViewModel
import org.koin.dsl.module

val commonModule = module {
    // 会话预创建服务
//    single<SessionPreCreationService> {
//        SessionPreCreationServiceImpl(
//            conversationRepository = get(),
//        )
//    }
    

}
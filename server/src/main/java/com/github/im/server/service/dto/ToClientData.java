package com.github.im.server.service.dto;

import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.model.proto.BaseMessage;

import java.util.Optional;

public interface ToClientData {


    /**
     * 获取发送到客户端的数据
     *
     *
     * 如果因参数问题 无法生成 返回即可， 参数校验 不在这里进行，
     * 有问题的数据 ，需要主动的在前置的流程里面 t hrow exception
     * @return 向客户端发送的数据
     */

    Optional<BaseMessage.BaseMessagePkg> toPkg();


    /**
     * 获取用户绑定在 Channel 的 BIndAttr
     * @return 每个链接所绑定的 key BindAttr  通常使用account + platform 组合标识
     */
    BindAttr<String> getBindAttr();
}

package com.github.im.server.config;

import java.util.UUID;

/**
 * 节点ID管理类
 * 用于生成和管理当前服务器节点的唯一标识符
 */
public class NodeId {
    /**
     * 当前节点的唯一标识符
     * 从环境变量NODE_ID获取，如果不存在则生成UUID
     */
    public static final String NODE_ID =
            System.getenv().getOrDefault("NODE_ID", UUID.randomUUID().toString());
}
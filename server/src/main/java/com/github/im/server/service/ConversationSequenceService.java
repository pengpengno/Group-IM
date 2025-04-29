package com.github.im.server.service;

/**
 * 会话的序列号自增服务
 */
public interface ConversationSequenceService {

    /**
     * 获取下一个 sequence
     * @param conversationId 会话ID
     * @return 当前会话下最新的 sequence
     */
    long nextSequence(Long conversationId);




    /**
     * 初始化 同步 DB 中的 会话 sequenceId 状态
     * 预防为主 ； 进程中每次 重启就同步  ，再redis 中形成一个检测的机制
     *  不存在 会话的序列号key 才会塞入
     *  需要考虑到：
     *  1. 集群中，多个进程都初始化，会存在重复的 sequenceId
     *  2. 启动过程中 会话量大时的 异步处理
     *  2.1 异步同步时 某个会话需要 传入消息时的优先加载策略 （补偿机制）
     *  2.2 异步同步的 retry 机制
     *  2.3 异步同步的分批处理队列
     */
    void initializeSequences();

}

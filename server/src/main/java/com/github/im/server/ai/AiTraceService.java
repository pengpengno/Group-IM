package com.github.im.server.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AI调用追踪服务
 */
@Service
public class AiTraceService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiTraceService.class);
    
    /**
     * 记录AI请求
     */
    public void traceRequest(String userId, String sessionId, String input, String output) {
        logger.info("AI Request - User: {}, Session: {}, Input: {}, Output: {}", 
                   userId, sessionId, input, output);
    }
    
    /**
     * 记录AI错误
     */
    public void traceError(String userId, String sessionId, String input, Exception ex) {
        logger.error("AI Error - User: {}, Session: {}, Input: {}, Error: {}", 
                    userId, sessionId, input, ex.getMessage(), ex);
    }
    
    /**
     * 记录工具调用
     */
    public void traceToolCall(String userId, String sessionId, String toolName, Object[] params, Object result) {
        logger.info("AI Tool Call - User: {}, Session: {}, Tool: {}, Params: {}, Result: {}", 
                   userId, sessionId, toolName, params, result);
    }
}
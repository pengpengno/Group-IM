package com.github.im.server.ai;

import com.github.im.server.ai.tool.UserInfoTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 工具注册表
 */
@Component
public class ToolRegistry {
    
    private final Map<String, Function<Object[], Object>> tools = new HashMap<>();
    private final UserInfoTool userInfoTool;
    
    @Autowired
    public ToolRegistry(UserInfoTool userInfoTool) {
        this.userInfoTool = userInfoTool;
        initializeTools();
    }
    
    private void initializeTools() {
        // 注册示例工具
        registerTool("userQuery", params -> {
            // 实现用户查询功能
            if (params.length > 0) {
                return userInfoTool.getUserInfo(params[0].toString());
            }
            return "Missing user ID parameter";
        });
        
        registerTool("chatHistory", params -> {
            // 实现聊天历史查询功能
            return "Chat history result";
        });
        
        registerTool("getUserContact", params -> {
            // 实现用户联系信息查询功能
            if (params.length > 0) {
                return userInfoTool.getUserContactInfo(params[0].toString());
            }
            return "Missing user ID parameter";
        });
    }
    
    /**
     * 注册工具
     */
    public void registerTool(String name, Function<Object[], Object> toolFunction) {
        tools.put(name, toolFunction);
    }
    
    /**
     * 调用工具
     */
    public Object invokeTool(String name, Object[] params) {
        if (!tools.containsKey(name)) {
            throw new IllegalArgumentException("Tool not found: " + name);
        }
        return tools.get(name).apply(params);
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
    
    /**
     * 获取所有工具名称
     */
    public Map<String, Function<Object[], Object>> getAllTools() {
        return new HashMap<>(tools);
    }
}
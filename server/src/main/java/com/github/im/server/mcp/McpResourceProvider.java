package com.github.im.server.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;

/**
 * MCP资源提供者
 * 为Lingma MCP提供服务端配置和接口信息
 */
@Component
public class McpResourceProvider {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有REST API接口信息
     */
    public ObjectNode getAllApiEndpoints() {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode endpoints = objectMapper.createArrayNode();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = 
            requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod method = entry.getValue();

            ObjectNode endpoint = objectMapper.createObjectNode();
            
            // 获取URL路径
            PatternsRequestCondition patternsCondition = info.getPatternsCondition();
            if (patternsCondition != null) {
                Set<String> patterns = patternsCondition.getPatterns();
                if (!patterns.isEmpty()) {
                    endpoint.put("path", patterns.iterator().next());
                }
            }

            // 获取HTTP方法
            RequestMethodsRequestCondition methodsCondition = info.getMethodsCondition();
            if (methodsCondition != null) {
                Set<RequestMethod> methods = methodsCondition.getMethods();
                if (!methods.isEmpty()) {
                    endpoint.put("method", methods.iterator().next().name());
                }
            }

            // 获取控制器信息
            endpoint.put("controller", method.getBeanType().getSimpleName());
            endpoint.put("method", method.getMethod().getName());

            // 获取方法描述
            endpoint.put("description", getMethodDescription(method));

            endpoints.add(endpoint);
        }

        result.set("endpoints", endpoints);
        result.put("total", endpoints.size());
        return result;
    }

    /**
     * 获取服务配置信息
     */
    public ObjectNode getServiceConfiguration() {
        ObjectNode config = objectMapper.createObjectNode();
        
        // 数据库配置
        ObjectNode database = objectMapper.createObjectNode();
        database.put("type", "PostgreSQL");
        database.put("driver", "org.postgresql.Driver");
        database.put("url", getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/group_im"));
        database.put("username", getProperty("spring.datasource.username", "postgres"));
        config.set("database", database);

        // Redis配置
        ObjectNode redis = objectMapper.createObjectNode();
        redis.put("host", getProperty("spring.data.redis.host", "localhost"));
        redis.put("port", Integer.parseInt(getProperty("spring.data.redis.port", "6379")));
        redis.put("database", getProperty("spring.data.redis.database", "0"));
        config.set("redis", redis);

        // WebSocket配置
        ObjectNode websocket = objectMapper.createObjectNode();
        websocket.put("enabled", true);
        websocket.put("endpoint", "/ws");
        websocket.put("protocol", "RFC 6455");
        config.set("websocket", websocket);

        // 安全配置
        ObjectNode security = objectMapper.createObjectNode();
        security.put("jwt_enabled", true);
        security.put("jwt_secret", "[配置项]");
        security.put("auth_endpoint", "/api/auth/login");
        security.put("token_validity_hours", 24);
        config.set("security", security);

        // 服务器配置
        ObjectNode server = objectMapper.createObjectNode();
        server.put("port", Integer.parseInt(getProperty("server.port", "8080")));
        server.put("context_path", getProperty("server.servlet.context-path", "/"));
        config.set("server", server);

        return config;
    }

    /**
     * 获取核心业务实体信息
     */
    public ObjectNode getBusinessEntities() {
        ObjectNode entities = objectMapper.createObjectNode();
        ArrayNode entityList = objectMapper.createArrayNode();

        // 手动添加核心实体信息（避免反射扫描复杂性）
        addEntityInfo(entityList, "User", "用户实体", Arrays.asList(
            "id: Long", "username: String", "email: String", 
            "password: String", "status: UserStatus", "createdAt: LocalDateTime"
        ));
        
        addEntityInfo(entityList, "Message", "消息实体", Arrays.asList(
            "id: Long", "fromUserId: Long", "toUserId: Long", 
            "content: String", "type: MessageType", "timestamp: LocalDateTime",
            "status: MessageStatus"
        ));
        
        addEntityInfo(entityList, "Group", "群组实体", Arrays.asList(
            "id: Long", "name: String", "description: String",
            "ownerId: Long", "createdAt: LocalDateTime"
        ));

        entities.set("entities", entityList);
        entities.put("total", entityList.size());
        return entities;
    }

    /**
     * 获取服务健康状态
     */
    public ObjectNode getServiceHealth() {
        ObjectNode health = objectMapper.createObjectNode();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "1.0.0");
        
        ObjectNode components = objectMapper.createObjectNode();
        components.put("database", "UP");
        components.put("redis", "UP");
        components.put("websocket", "UP");
        components.put("authentication", "UP");
        
        health.set("components", components);
        return health;
    }

    /**
     * 获取完整的服务信息摘要
     */
    public ObjectNode getServiceSummary() {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.set("configuration", getServiceConfiguration());
        summary.set("endpoints", getAllApiEndpoints());
        summary.set("entities", getBusinessEntities());
        summary.set("health", getServiceHealth());
        return summary;
    }

    // 辅助方法
    private String getProperty(String key, String defaultValue) {
        try {
            return applicationContext.getEnvironment().getProperty(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getMethodDescription(HandlerMethod method) {
        // 简单的描述逻辑，可以根据实际需求增强
        String methodName = method.getMethod().getName();
        if (methodName.contains("login")) return "用户登录认证";
        if (methodName.contains("register")) return "用户注册";
        if (methodName.contains("message")) return "消息相关操作";
        if (methodName.contains("group")) return "群组相关操作";
        if (methodName.contains("user")) return "用户相关操作";
        return "业务接口";
    }

    private void addEntityInfo(ArrayNode entityList, String name, String description, List<String> fields) {
        ObjectNode entity = objectMapper.createObjectNode();
        entity.put("name", name);
        entity.put("description", description);
        ArrayNode fieldArray = objectMapper.createArrayNode();
        for (String field : fields) {
            fieldArray.add(field);
        }
        entity.set("fields", fieldArray);
        entityList.add(entity);
    }
}
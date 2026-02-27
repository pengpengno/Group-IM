package com.github.im.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCP信息服务控制器 - 支持JSON-RPC协议
 * 为Lingma MCP提供服务端配置和接口信息
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp")
@Tag(name = "MCP信息服务", description = "为AI助手提供服务端信息")
public class McpInfoController {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * MCP工具调用 - 支持JSON-RPC协议
     * 处理Lingma发送的JSON-RPC格式请求
     */
    @RequestMapping(value = "/tools/call", method = {RequestMethod.GET, RequestMethod.POST}, 
                   produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "MCP工具调用", description = "处理MCP工具调用请求")
    public SseEmitter handleToolCall(@RequestBody(required = false) JsonNode request) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        executor.execute(() -> {
            try {
                log.info("Received MCP request: {}", request);
                
                // 处理GET请求或空请求体
                if (request == null || request.isNull()) {
                    sendToolResult(emitter, "tools_list", getToolsListData());
                    emitter.complete();
                    return;
                }

                // 处理JSON-RPC initialize请求
                if (request.has("method") && "initialize".equals(request.get("method").asText())) {
                    sendInitializeResponse(emitter, request);
                    return;
                }

                // 处理JSON-RPC工具调用请求
                if (request.has("method") && "tools/call".equals(request.get("method").asText())) {
                    String toolName = request.path("params").path("name").asText();
                    handleToolCall(emitter, toolName);
                    return;
                }

                // 处理工具列表请求
                if (request.has("method") && "tools/list".equals(request.get("method").asText())) {
                    sendToolListResponse(emitter, request);
                    return;
                }

                // 默认情况：返回工具列表
                sendToolResult(emitter, "tools_list", getToolsListData());
                emitter.complete();
                
            } catch (Exception e) {
                log.error("Error processing MCP request: ", e);
                try {
                    sendError(emitter, "Internal error: " + e.getMessage());
                    emitter.complete();
                } catch (IOException ioException) {
                    log.error("Failed to send error message: ", ioException);
                }
            }
        });
        
        return emitter;
    }

    /**
     * 处理工具调用
     */
    private void handleToolCall(SseEmitter emitter, String toolName) throws IOException {
        switch (toolName) {
            case "get_service_summary":
                sendToolResult(emitter, "get_service_summary", getServiceSummaryData());
                break;
            case "get_api_endpoints":
                sendToolResult(emitter, "get_api_endpoints", getApiEndpointsData());
                break;
            case "get_service_config":
                sendToolResult(emitter, "get_service_config", getServiceConfigData());
                break;
            case "get_business_entities":
                sendToolResult(emitter, "get_business_entities", getBusinessEntitiesData());
                break;
            case "get_service_health":
                sendToolResult(emitter, "get_service_health", getServiceHealthData());
                break;
            default:
                sendError(emitter, "Unknown tool: " + toolName);
        }
        emitter.complete();
    }

    /**
     * 发送初始化响应
     */
    private void sendInitializeResponse(SseEmitter emitter, JsonNode request) throws IOException {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", request.path("id").asInt());
        
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");
        
        ObjectNode capabilities = objectMapper.createObjectNode();
        ObjectNode toolsCapability = objectMapper.createObjectNode();
        toolsCapability.put("listChanged", true);
        capabilities.set("tools", toolsCapability);
        
        result.set("capabilities", capabilities);
        response.set("result", result);
        
        emitter.send(SseEmitter.event().name("message").data(response.toString()));
        emitter.complete();
    }

    /**
     * 发送工具列表响应
     */
    private void sendToolListResponse(SseEmitter emitter, JsonNode request) throws IOException {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", request.path("id").asInt());
        
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode toolsArray = objectMapper.createArrayNode();
        
        // 添加工具定义
        toolsArray.add(createRpcToolDefinition("get_service_summary", "获取服务端完整摘要信息"));
        toolsArray.add(createRpcToolDefinition("get_api_endpoints", "获取所有REST API接口列表"));
        toolsArray.add(createRpcToolDefinition("get_service_config", "获取服务端配置信息"));
        toolsArray.add(createRpcToolDefinition("get_business_entities", "获取核心业务实体结构"));
        toolsArray.add(createRpcToolDefinition("get_service_health", "获取服务组件健康状态"));
        
        result.set("tools", toolsArray);
        response.set("result", result);
        
        emitter.send(SseEmitter.event().name("message").data(response.toString()));
        emitter.complete();
    }

    /**
     * 获取所有可用工具列表 (REST endpoint)
     */
    @GetMapping(value = "/tools/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取工具列表", description = "返回所有可用的MCP工具")
    public ObjectNode getToolsList() {
        ObjectNode tools = objectMapper.createObjectNode();
        ObjectNode toolsArray = objectMapper.createObjectNode();
        
        // 添加工具定义
        toolsArray.set("get_service_summary", createToolDefinition(
            "获取服务端完整摘要信息", 
            "返回服务配置、接口、实体和健康状态等完整信息"));
        toolsArray.set("get_api_endpoints", createToolDefinition(
            "获取所有REST API接口列表", 
            "返回所有REST API接口的详细信息"));
        toolsArray.set("get_service_config", createToolDefinition(
            "获取服务端配置信息", 
            "返回数据库、Redis等基础设施配置详情"));
        toolsArray.set("get_business_entities", createToolDefinition(
            "获取核心业务实体结构", 
            "返回用户、消息、群组等核心业务实体信息"));
        toolsArray.set("get_service_health", createToolDefinition(
            "获取服务组件健康状态", 
            "返回各组件的运行状态和健康检查信息"));
            
        tools.set("tools", toolsArray);
        return tools;
    }

    // 工具实现方法
    private ObjectNode getServiceSummaryData() {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.set("configuration", getServiceConfiguration());
        summary.set("endpoints", getAllApiEndpoints());
        summary.set("entities", getBusinessEntities());
        summary.set("health", getServiceHealth());
        return summary;
    }

    private ObjectNode getApiEndpointsData() {
        return getAllApiEndpoints();
    }

    private ObjectNode getServiceConfigData() {
        return getServiceConfiguration();
    }

    private ObjectNode getBusinessEntitiesData() {
        return getBusinessEntities();
    }

    private ObjectNode getServiceHealthData() {
        return getServiceHealth();
    }

    // GET请求时返回的工具列表数据
    private ObjectNode getToolsListData() {
        return getToolsList();
    }

    // 辅助方法
    private ObjectNode createToolDefinition(String name, String description) {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("input_schema", "{}");
        return tool;
    }

    private ObjectNode createRpcToolDefinition(String name, String description) {
        ObjectNode tool = objectMapper.createObjectNode();
        tool.put("name", name);
        tool.put("description", description);
        ObjectNode inputSchema = objectMapper.createObjectNode();
        inputSchema.put("type", "object");
        tool.set("inputSchema", inputSchema);
        return tool;
    }

    private void sendToolResult(SseEmitter emitter, String toolName, ObjectNode result) throws IOException {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("tool_name", toolName);
        response.set("result", result);
        emitter.send(SseEmitter.event().name("message").data(response.toString()));
    }

    private void sendError(SseEmitter emitter, String errorMessage) throws IOException {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", errorMessage);
        emitter.send(SseEmitter.event().name("error").data(error.toString()));
    }

    // 保留原有的同步接口作为备用
    @RequestMapping(value = "/summary", method = {RequestMethod.GET, RequestMethod.POST}, 
                   produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取服务摘要信息", description = "返回服务配置、接口、实体等完整信息")
    public ResponseEntity<ObjectNode> getServiceSummary() {
        return ResponseEntity.ok(getServiceSummaryData());
    }

    @RequestMapping(value = "/config", method = {RequestMethod.GET, RequestMethod.POST},
                   produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取服务配置", description = "返回数据库、Redis、WebSocket等配置信息")
    public ResponseEntity<ObjectNode> getServiceConfig() {
        return ResponseEntity.ok(getServiceConfigData());
    }

    @RequestMapping(value = "/endpoints", method = {RequestMethod.GET, RequestMethod.POST},
                   produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取所有API端点", description = "返回所有REST API接口信息")
    public ResponseEntity<ObjectNode> getAllEndpoints() {
        return ResponseEntity.ok(getApiEndpointsData());
    }

    @RequestMapping(value = "/entities", method = {RequestMethod.GET, RequestMethod.POST},
                   produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取业务实体", description = "返回核心业务实体结构信息")
    public ResponseEntity<ObjectNode> getEntities() {
        return ResponseEntity.ok(getBusinessEntitiesData());
    }

    @RequestMapping(value = "/health", method = {RequestMethod.GET, RequestMethod.POST},
                   produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取服务健康状态", description = "返回服务组件运行状态")
    public ResponseEntity<ObjectNode> getHealth() {
        return ResponseEntity.ok(getServiceHealthData());
    }

    // 数据获取方法保持不变...
    private ObjectNode getAllApiEndpoints() {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode endpoints = objectMapper.createArrayNode();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = 
            requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod method = entry.getValue();

            ObjectNode endpoint = objectMapper.createObjectNode();
            
            PatternsRequestCondition patternsCondition = info.getPatternsCondition();
            if (patternsCondition != null) {
                Set<String> patterns = patternsCondition.getPatterns();
                if (!patterns.isEmpty()) {
                    endpoint.put("path", patterns.iterator().next());
                }
            }

            RequestMethodsRequestCondition methodsCondition = info.getMethodsCondition();
            if (methodsCondition != null) {
                Set<org.springframework.web.bind.annotation.RequestMethod> methods = methodsCondition.getMethods();
                if (!methods.isEmpty()) {
                    endpoint.put("method", methods.iterator().next().name());
                }
            }

            endpoint.put("controller", method.getBeanType().getSimpleName());
            endpoint.put("method", method.getMethod().getName());
            endpoint.put("description", getMethodDescription(method));

            endpoints.add(endpoint);
        }

        result.set("endpoints", endpoints);
        result.put("total", endpoints.size());
        return result;
    }

    private ObjectNode getServiceConfiguration() {
        ObjectNode config = objectMapper.createObjectNode();
        ObjectNode database = objectMapper.createObjectNode();
        database.put("type", "PostgreSQL");
        database.put("driver", "org.postgresql.Driver");
        database.put("connection_example", "jdbc:postgresql://localhost:5432/group_im");
        database.put("username", "postgres");
        config.set("database", database);

        ObjectNode redis = objectMapper.createObjectNode();
        redis.put("host", "localhost");
        redis.put("port", 6379);
        redis.put("database", 0);
        config.set("redis", redis);

        ObjectNode websocket = objectMapper.createObjectNode();
        websocket.put("enabled", true);
        websocket.put("endpoint", "/ws");
        websocket.put("protocol", "RFC 6455");
        config.set("websocket", websocket);

        ObjectNode security = objectMapper.createObjectNode();
        security.put("jwt_enabled", true);
        security.put("auth_endpoint", "/api/auth/login");
        security.put("token_validity_hours", 24);
        config.set("security", security);

        ObjectNode server = objectMapper.createObjectNode();
        server.put("port", 8080);
        server.put("context_path", "/");
        config.set("server", server);

        return config;
    }

    private ObjectNode getBusinessEntities() {
        ObjectNode entities = objectMapper.createObjectNode();
        ArrayNode entityList = objectMapper.createArrayNode();

        addEntityInfo(entityList, "User", "用户实体，存储用户基本信息", 
            "id: Long - 用户ID", "username: String - 用户名", "email: String - 邮箱",
            "password: String - 密码(加密存储)", "status: UserStatus - 用户状态",
            "createdAt: LocalDateTime - 创建时间");
        
        addEntityInfo(entityList, "Message", "消息实体，存储聊天消息", 
            "id: Long - 消息ID", "fromUserId: Long - 发送者ID", "toUserId: Long - 接收者ID",
            "content: String - 消息内容", "type: MessageType - 消息类型",
            "timestamp: LocalDateTime - 时间戳", "status: MessageStatus - 消息状态");
        
        addEntityInfo(entityList, "Group", "群组实体，存储群聊信息",
            "id: Long - 群组ID", "name: String - 群名称", "description: String - 群描述",
            "ownerId: Long - 群主ID", "createdAt: LocalDateTime - 创建时间");

        entities.set("entities", entityList);
        entities.put("total", entityList.size());
        return entities;
    }

    private ObjectNode getServiceHealth() {
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

    private String getMethodDescription(HandlerMethod method) {
        String methodName = method.getMethod().getName().toLowerCase();
        if (methodName.contains("login")) return "用户登录认证接口";
        if (methodName.contains("register")) return "用户注册接口";
        if (methodName.contains("message")) return "消息相关操作接口";
        if (methodName.contains("group")) return "群组相关操作接口";
        if (methodName.contains("user")) return "用户相关操作接口";
        if (methodName.contains("auth")) return "认证相关接口";
        return "业务接口";
    }

    private void addEntityInfo(ArrayNode entityList, String name, String description, String... fields) {
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
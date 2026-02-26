package com.github.im.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.im.server.mcp.McpResourceProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP工具使用示例控制器
 * 展示如何通过REST API调用GroupIM的MCP工具
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp/demo")
@RequiredArgsConstructor
@Tag(name = "MCP工具演示", description = "GroupIM MCP工具使用示例")
public class McpDemoController {

    private final McpResourceProvider mcpResourceProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取服务摘要信息 - 演示 get_service_summary 工具
     */
    @GetMapping("/service-summary")
    @Operation(summary = "获取服务摘要", description = "调用 get_service_summary 工具获取完整服务信息")
    public ResponseEntity<JsonNode> getServiceSummary() {
        log.info("调用 get_service_summary 工具");
        ObjectNode summary = mcpResourceProvider.getServiceSummary();
        
        // 添加调用信息
        ObjectNode result = objectMapper.createObjectNode();
        result.set("service_summary", summary);
        result.put("tool_called", "get_service_summary");
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取API端点列表 - 演示 get_api_endpoints 工具
     */
    @GetMapping("/api-endpoints")
    @Operation(summary = "获取API端点", description = "调用 get_api_endpoints 工具获取所有REST API接口")
    public ResponseEntity<JsonNode> getApiEndpoints() {
        log.info("调用 get_api_endpoints 工具");
        ObjectNode endpoints = mcpResourceProvider.getAllApiEndpoints();
        
        ObjectNode result = objectMapper.createObjectNode();
        result.set("api_endpoints", endpoints);
        result.put("tool_called", "get_api_endpoints");
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取服务配置 - 演示 get_service_config 工具
     */
    @GetMapping("/service-config")
    @Operation(summary = "获取服务配置", description = "调用 get_service_config 工具获取服务端配置信息")
    public ResponseEntity<JsonNode> getServiceConfig() {
        log.info("调用 get_service_config 工具");
        ObjectNode config = mcpResourceProvider.getServiceConfiguration();
        
        ObjectNode result = objectMapper.createObjectNode();
        result.set("service_config", config);
        result.put("tool_called", "get_service_config");
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取业务实体信息 - 演示 get_business_entities 工具
     */
    @GetMapping("/business-entities")
    @Operation(summary = "获取业务实体", description = "调用 get_business_entities 工具获取核心业务实体结构")
    public ResponseEntity<JsonNode> getBusinessEntities() {
        log.info("调用 get_business_entities 工具");
        ObjectNode entities = mcpResourceProvider.getBusinessEntities();
        
        ObjectNode result = objectMapper.createObjectNode();
        result.set("business_entities", entities);
        result.put("tool_called", "get_business_entities");
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取服务健康状态 - 演示 get_service_health 工具
     */
    @GetMapping("/service-health")
    @Operation(summary = "获取服务健康", description = "调用 get_service_health 工具获取服务组件健康状态")
    public ResponseEntity<JsonNode> getServiceHealth() {
        log.info("调用 get_service_health 工具");
        ObjectNode health = mcpResourceProvider.getServiceHealth();
        
        ObjectNode result = objectMapper.createObjectNode();
        result.set("service_health", health);
        result.put("tool_called", "get_service_health");
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 批量调用多个MCP工具
     */
    @PostMapping("/batch-call")
    @Operation(summary = "批量调用工具", description = "一次调用多个MCP工具获取综合信息")
    public ResponseEntity<JsonNode> batchCallTools(@RequestBody(required = false) Map<String, Boolean> toolsToCall) {
        log.info("批量调用MCP工具: {}", toolsToCall);
        
        ObjectNode result = objectMapper.createObjectNode();
        result.put("timestamp", System.currentTimeMillis());
        ObjectNode toolsResult = objectMapper.createObjectNode();
        
        // 默认调用所有工具
        if (toolsToCall == null || toolsToCall.isEmpty()) {
            toolsToCall = new HashMap<>();
            toolsToCall.put("service_summary", true);
            toolsToCall.put("api_endpoints", true);
            toolsToCall.put("service_config", true);
            toolsToCall.put("business_entities", true);
            toolsToCall.put("service_health", true);
        }
        
        // 根据请求调用相应工具
        if (toolsToCall.getOrDefault("service_summary", false)) {
            toolsResult.set("service_summary", mcpResourceProvider.getServiceSummary());
        }
        
        if (toolsToCall.getOrDefault("api_endpoints", false)) {
            toolsResult.set("api_endpoints", mcpResourceProvider.getAllApiEndpoints());
        }
        
        if (toolsToCall.getOrDefault("service_config", false)) {
            toolsResult.set("service_config", mcpResourceProvider.getServiceConfiguration());
        }
        
        if (toolsToCall.getOrDefault("business_entities", false)) {
            toolsResult.set("business_entities", mcpResourceProvider.getBusinessEntities());
        }
        
        if (toolsToCall.getOrDefault("service_health", false)) {
            toolsResult.set("service_health", mcpResourceProvider.getServiceHealth());
        }
        
        result.set("tools_result", toolsResult);
        result.put("tools_called", toolsToCall.toString());
        
        return ResponseEntity.ok(result);
    }

    /**
     * MCP工具调用入口点（符合Lingma MCP规范）
     */
    @PostMapping("/call-tool")
    @Operation(summary = "调用指定工具", description = "按照MCP协议调用指定的工具")
    public ResponseEntity<JsonNode> callTool(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("tool_name");
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
        
        log.info("调用MCP工具: {}, 参数: {}", toolName, arguments);
        
        ObjectNode result = objectMapper.createObjectNode();
        ObjectNode toolResult = objectMapper.createObjectNode();
        
        try {
            switch (toolName) {
                case "get_service_summary":
                    toolResult = mcpResourceProvider.getServiceSummary();
                    break;
                case "get_api_endpoints":
                    toolResult = mcpResourceProvider.getAllApiEndpoints();
                    break;
                case "get_service_config":
                    toolResult = mcpResourceProvider.getServiceConfiguration();
                    break;
                case "get_business_entities":
                    toolResult = mcpResourceProvider.getBusinessEntities();
                    break;
                case "get_service_health":
                    toolResult = mcpResourceProvider.getServiceHealth();
                    break;
                default:
                    return ResponseEntity.badRequest()
                        .body(createErrorResult("未知的工具名称: " + toolName));
            }
            
            result.set("result", toolResult);
            result.put("success", true);
            result.put("tool_name", toolName);
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("调用工具 {} 失败", toolName, e);
            return ResponseEntity.internalServerError()
                .body(createErrorResult("工具调用失败: " + e.getMessage()));
        }
        
        return ResponseEntity.ok(result);
    }
    
    private ObjectNode createErrorResult(String errorMessage) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("success", false);
        error.put("error", errorMessage);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
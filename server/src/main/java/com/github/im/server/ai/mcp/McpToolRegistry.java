package com.github.im.server.ai.mcp;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One registry for robot and future external-MCP tool discovery. */
@Component
public class McpToolRegistry {
    private final Map<String, McpToolHandler> tools;

    public McpToolRegistry(List<McpToolHandler> handlers) {
        Map<String, McpToolHandler> registered = new LinkedHashMap<>();
        for (McpToolHandler handler : handlers) {
            String name = handler.descriptor().name();
            if (registered.putIfAbsent(name, handler) != null) {
                throw new IllegalStateException("Duplicate MCP tool registration: " + name);
            }
        }
        this.tools = Map.copyOf(registered);
    }

    public McpToolHandler require(String name) {
        McpToolHandler tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("MCP tool is not registered: " + name);
        }
        return tool;
    }

    public Collection<McpToolDescriptor> listDescriptors() {
        return tools.values().stream().map(McpToolHandler::descriptor).toList();
    }
}

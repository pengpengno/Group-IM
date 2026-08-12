package com.github.im.server.ai.mcp;

/** Risk classification used to decide whether a tool can run without approval. */
public enum McpToolRisk {
    READ_ONLY,
    SENSITIVE_READ,
    WRITE
}

package com.github.im.server.ai.mcp;

import org.springframework.stereotype.Component;

/** Default-deny policy for any operation that can disclose sensitive data or write state. */
@Component
public class ToolAccessPolicy {
    public void requireAllowed(McpToolInvocation invocation, McpToolDescriptor descriptor) {
        if (!invocation.context().hasAuthenticatedActor()) {
            throw new SecurityException("Authenticated actor is required");
        }
        if (descriptor.risk() != McpToolRisk.READ_ONLY && !invocation.context().confirmationGranted()) {
            throw new McpConfirmationRequiredException(descriptor.name());
        }
    }
}

package com.github.im.server.workbench.integration.notification;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Release-owned switches for Workbench notification delivery.
 *
 * All switches default to false. Enabling a channel also requires both
 * supported-client release floors to be recorded, so a partial deployment
 * cannot accidentally start emission.
 */
@Configuration
@ConfigurationProperties(prefix = "group.workbench.rollout")
@Data
public class WorkbenchRolloutProperties {

    private boolean enabled = false;
    private boolean clientEventEnabled = false;
    private boolean pushEnabled = false;
    private boolean imCardEnabled = false;
    private String minimumWebVersion = "";
    private String minimumAndroidVersion = "";
}

package com.github.im.server.config.mult;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class TenantHolder {
    private String tenant;
    public String getTenant() { return tenant; }
    public void setTenant(String tenant) { this.tenant = tenant; }
}

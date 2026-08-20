package com.github.im.server.listener;

import com.github.im.server.event.CompanyCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Post-provisioning extension point for company metadata/cache refresh work.
 *
 * Tenant DDL must not run from this event listener. New-company schema creation and
 * Flyway migrations are completed synchronously before CompanyCreatedEvent is published.
 */
@Component
@Slf4j
public class CompanyCreatedEventListener {

    @EventListener
    public void handleCompanyCreatedEvent(CompanyCreatedEvent event) {
        log.info(
                "Company created and tenant provisioning completed: companyId={}, schema={}, active={}",
                event.getCompany().getCompanyId(),
                event.getCompany().getSchemaName(),
                event.getCompany().getActive()
        );
    }
}

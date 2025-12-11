package com.github.im.server.event;

import com.github.im.server.model.Company;
import org.springframework.context.ApplicationEvent;

public class CompanyCreatedEvent extends ApplicationEvent {
    private final Company company;

    public CompanyCreatedEvent(Company company) {
        super(company);
        this.company = company;
    }

    public Company getCompany() {
        return company;
    }
}
package com.bes.jira.plugins.oauth2bridge.impl;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.bes.jira.plugins.oauth2bridge.api.Oauth2BridgeComponent;

import javax.inject.Inject;
import javax.inject.Named;

@ExportAsService({Oauth2BridgeComponent.class})
@Named("oauth2BridgeComponent")
public class Oauth2BridgeComponentImpl implements Oauth2BridgeComponent {
    @ComponentImport
    private final ApplicationProperties applicationProperties;

    @Inject

    public Oauth2BridgeComponentImpl(final ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public String getName() {
        if (null != applicationProperties) {
            return "oauth2BridgeComponent:" + applicationProperties.getDisplayName();
        }

        return "oauth2BridgeComponent";
    }
}
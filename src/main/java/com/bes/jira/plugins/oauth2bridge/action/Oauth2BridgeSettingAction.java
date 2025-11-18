package com.bes.jira.plugins.oauth2bridge.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2BridgeConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class Oauth2BridgeSettingAction extends JiraWebActionSupport {

    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeSettingAction.class);

    private final Oauth2BridgeConfigService configService;

    private String userInfoEndpoint;

    @Inject
    public Oauth2BridgeSettingAction(Oauth2BridgeConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String doDefault() {
        userInfoEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT);

        log.info("Loaded config: userInfoEndpoint={}", userInfoEndpoint);

        return INPUT;
    }

    @Override
    public String doExecute() {
        log.info("Saving config: clientId={}", userInfoEndpoint);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT, userInfoEndpoint);
        return SUCCESS;
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }
}
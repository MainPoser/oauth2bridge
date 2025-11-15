package com.bes.jira.plugins.oauth2bridge.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2BridgeConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class Oauth2BridgeConfigAction extends JiraWebActionSupport {

    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeConfigAction.class);

    private final Oauth2BridgeConfigService configService;

    private String clientId;
    private String clientSecret;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;

    @Inject
    public Oauth2BridgeConfigAction(Oauth2BridgeConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String doDefault() {
        clientId = configService.getConfig("clientId");
        clientSecret = configService.getConfig("clientSecret");
        authorizationEndpoint = configService.getConfig("authorizationEndpoint");
        tokenEndpoint = configService.getConfig("tokenEndpoint");
        userInfoEndpoint = configService.getConfig("userInfoEndpoint");

        log.info("Loaded config: clientId={}, clientSecret={}, authEndpoint={}, tokenEndpoint={}, userInfoEndpoint={}",
                clientId, mask(clientSecret), authorizationEndpoint, tokenEndpoint, userInfoEndpoint);

        return INPUT;
    }

    @Override
    public String doExecute() {
        log.info("Saving config: clientId={}, clientSecret={}, authEndpoint={}, tokenEndpoint={}, userInfoEndpoint={}",
                clientId, mask(clientSecret), authorizationEndpoint, tokenEndpoint, userInfoEndpoint);
        // 可加入字段校验
        configService.saveConfig("clientId", clientId);
        configService.saveConfig("clientSecret", clientSecret);
        configService.saveConfig("authorizationEndpoint", authorizationEndpoint);
        configService.saveConfig("tokenEndpoint", tokenEndpoint);
        configService.saveConfig("userInfoEndpoint", userInfoEndpoint);
        return SUCCESS;
    }

    // getter/setter
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    // 遮掩敏感信息
    private String mask(String s) {
        if (s == null || s.length() <= 4) return "****";
        return s.substring(0, 2) + "****" + s.substring(s.length() - 2);
    }
}
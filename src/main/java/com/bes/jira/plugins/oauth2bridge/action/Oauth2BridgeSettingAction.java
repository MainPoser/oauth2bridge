package com.bes.jira.plugins.oauth2bridge.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2BridgeConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class Oauth2BridgeSettingAction extends JiraWebActionSupport {

    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeSettingAction.class);

    private Oauth2BridgeConfigService configService;

    private String clientId;
    private String clientSecret;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;

    public Oauth2BridgeSettingAction() {
        // 必须保留无参构造器供 Webwork1 初始化。不能使用@Inject注入有参构造器
        log.info("Used Cont");
    }

    @Inject
    public void setConfigService(Oauth2BridgeConfigService configService) {
        log.info("Set ConfigService");
        this.configService = configService;
    }

    @Override
    public String doDefault() {
        clientId = configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID);
        clientSecret = configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_SECRET);
        authorizationEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_AUTHORIZATION_ENDPOINT);
        tokenEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_TOKEN_ENDPOINT);
        userInfoEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT);

        log.info("Loaded config: clientId={}, clientSecret={}, authEndpoint={}, tokenEndpoint={}, userInfoEndpoint={}",
                clientId, mask(clientSecret), authorizationEndpoint, tokenEndpoint, userInfoEndpoint);

        return INPUT;
    }

    @Override
    public String doExecute() {
        log.info("Saving config: clientId={}, clientSecret={}, authEndpoint={}, tokenEndpoint={}, userInfoEndpoint={}",
                clientId, mask(clientSecret), authorizationEndpoint, tokenEndpoint, userInfoEndpoint);
        // 可加入字段校验
        configService.saveConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID, clientId);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_CLIENT_SECRET, clientSecret);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_AUTHORIZATION_ENDPOINT, authorizationEndpoint);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_TOKEN_ENDPOINT, tokenEndpoint);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT, userInfoEndpoint);
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
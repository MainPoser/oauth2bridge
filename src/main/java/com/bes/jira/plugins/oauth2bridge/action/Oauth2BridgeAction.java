package com.bes.jira.plugins.oauth2bridge.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2BridgeConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class Oauth2BridgeAction extends JiraWebActionSupport {

    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeAction.class);

    private final Oauth2BridgeConfigService configService;

    private String userInfoEndpoint;
    public boolean insecureSkipVerify;
    public String trustCaCert;
    public long sessionTimeoutSec;

    @Inject
    public Oauth2BridgeAction(Oauth2BridgeConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String doDefault() {
        userInfoEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT);
        insecureSkipVerify = Boolean.parseBoolean(configService.getConfig(Oauth2BridgeConfigService.KEY_INSECURE_SKIP_VERIFY));
        trustCaCert = configService.getConfig(Oauth2BridgeConfigService.KEY_TRUST_CA_CERT);
        try {
            sessionTimeoutSec = Long.parseLong(configService.getConfig(Oauth2BridgeConfigService.KEY_SESSION_TIMEOUT_SEC));
        } catch (NumberFormatException e) {
            log.warn("Parse sessionTimeoutSec failed, use default");
        }

        // 如果为空，设置默认值
        if (sessionTimeoutSec <= 0) {
            sessionTimeoutSec = 30 * 60;
            configService.saveConfig(Oauth2BridgeConfigService.KEY_SESSION_TIMEOUT_SEC, String.valueOf(sessionTimeoutSec));
        }
        log.debug("Loaded config: userInfoEndpoint={},insecureSkipVerify={},trustCaCert={},sessionTimeoutSec={}",
                userInfoEndpoint, insecureSkipVerify, trustCaCert, sessionTimeoutSec);
        return INPUT;
    }

    @Override
    public String doExecute() {
        if (this.command == null || this.command.isEmpty()){
            return  this.doDefault();
        }
        log.debug("Saving config: userInfoEndpoint={},insecureSkipVerify={},trustCaCert={},sessionTimeoutSec={}",
                userInfoEndpoint, insecureSkipVerify, trustCaCert, sessionTimeoutSec);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_USERINFO_ENDPOINT, userInfoEndpoint);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_INSECURE_SKIP_VERIFY, String.valueOf(insecureSkipVerify));
        configService.saveConfig(Oauth2BridgeConfigService.KEY_TRUST_CA_CERT, trustCaCert);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_SESSION_TIMEOUT_SEC, String.valueOf(sessionTimeoutSec));
        return SUCCESS;
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    public boolean isInsecureSkipVerify() {
        return insecureSkipVerify;
    }

    public void setInsecureSkipVerify(boolean insecureSkipVerify) {
        this.insecureSkipVerify = insecureSkipVerify;
    }

    public String getTrustCaCert() {
        return trustCaCert;
    }

    public void setTrustCaCert(String trustCaCert) {
        this.trustCaCert = trustCaCert;
    }

    public long getSessionTimeoutSec() {
        return sessionTimeoutSec;
    }

    public void setSessionTimeoutSec(long sessionTimeoutSec) {
        this.sessionTimeoutSec = sessionTimeoutSec;
    }
}
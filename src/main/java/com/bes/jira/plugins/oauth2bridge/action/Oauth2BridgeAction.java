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

    private String introspectionEndpoint;
    private String clientId;
    private String clientSecret;
    public boolean insecureSkipVerify;
    public String trustCaCert;
    public long sessionTimeoutSec;

    @Inject
    public Oauth2BridgeAction(Oauth2BridgeConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String doDefault() {
        introspectionEndpoint = configService.getConfig(Oauth2BridgeConfigService.KEY_INTROSPECTION_ENDPOINT);
        clientId = configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID);
        clientSecret = configService.getConfig(Oauth2BridgeConfigService.KEY_CLIENT_SECRET);
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
        log.debug("Loaded config: introspectionEndpoint={},clientId={},clientSecret={},insecureSkipVerify={},trustCaCert={},sessionTimeoutSec={}",
                introspectionEndpoint, clientId, clientSecret, insecureSkipVerify, trustCaCert, sessionTimeoutSec);
        return INPUT;
    }

    @Override
    public String doExecute() {
        if (this.command == null || this.command.isEmpty()) {
            return this.doDefault();
        }
        log.debug("Saving config: introspectionEndpoint={},clientId={},clientSecret={},insecureSkipVerify={},trustCaCert={},sessionTimeoutSec={}",
                introspectionEndpoint, clientId, clientSecret, insecureSkipVerify, trustCaCert, sessionTimeoutSec);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_INTROSPECTION_ENDPOINT, introspectionEndpoint);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_CLIENT_ID, clientId);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_CLIENT_SECRET, clientSecret);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_INSECURE_SKIP_VERIFY, String.valueOf(insecureSkipVerify));
        configService.saveConfig(Oauth2BridgeConfigService.KEY_TRUST_CA_CERT, trustCaCert);
        configService.saveConfig(Oauth2BridgeConfigService.KEY_SESSION_TIMEOUT_SEC, String.valueOf(sessionTimeoutSec));
        return SUCCESS;
    }

    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

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
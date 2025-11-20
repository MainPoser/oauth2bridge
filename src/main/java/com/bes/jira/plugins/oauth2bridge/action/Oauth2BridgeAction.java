package com.bes.jira.plugins.oauth2bridge.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.bes.jira.plugins.oauth2bridge.model.Oauth2BridgeConfig;
import com.bes.jira.plugins.oauth2bridge.service.Oauth2BridgeConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

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
        Oauth2BridgeConfig config = configService.getConfig();
        introspectionEndpoint = config.getIntrospectionEndpoint();
        clientId = config.getClientId();
        clientSecret = config.getClientSecret();
        insecureSkipVerify = config.isInsecureSkipVerify();
        trustCaCert = config.getIntrospectionEndpoint();
        sessionTimeoutSec = config.getSessionTimeoutSec();
        log.debug("Loaded config: {}", config);
        return INPUT;
    }

    @Override
    public String doExecute() throws IOException {
        if (this.command == null || this.command.isEmpty()) {
            return this.doDefault();
        }
        log.debug("Saving config: introspectionEndpoint={},clientId={},clientSecret={},insecureSkipVerify={},trustCaCert={},sessionTimeoutSec={}",
                introspectionEndpoint, clientId, clientSecret, insecureSkipVerify, trustCaCert, sessionTimeoutSec);
        Oauth2BridgeConfig oauth2BridgeConfig = new Oauth2BridgeConfig(introspectionEndpoint, clientId, clientSecret, insecureSkipVerify, trustCaCert, sessionTimeoutSec);
        configService.saveConfig(oauth2BridgeConfig);
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
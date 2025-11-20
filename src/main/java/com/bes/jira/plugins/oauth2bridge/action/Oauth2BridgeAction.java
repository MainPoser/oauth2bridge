package com.bes.jira.plugins.oauth2bridge.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.bes.jira.plugins.oauth2bridge.model.Oauth2BridgeSetting;
import com.bes.jira.plugins.oauth2bridge.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

@Named
public class Oauth2BridgeAction extends JiraWebActionSupport {

    private static final Logger log = LoggerFactory.getLogger(Oauth2BridgeAction.class);

    private final SettingService settingService;

    private String introspectionEndpoint;
    private String clientId;
    private String clientSecret;
    public boolean insecureSkipVerify;
    public String trustCaCert;
    public long sessionTimeoutSec;

    @Inject
    public Oauth2BridgeAction(SettingService settingService) {
        this.settingService = settingService;
    }

    @Override
    public String doDefault() {
        Oauth2BridgeSetting setting = settingService.getSetting();
        introspectionEndpoint = setting.getIntrospectionEndpoint();
        clientId = setting.getClientId();
        clientSecret = setting.getClientSecret();
        insecureSkipVerify = setting.isInsecureSkipVerify();
        trustCaCert = setting.getIntrospectionEndpoint();
        sessionTimeoutSec = setting.getSessionTimeoutSec();
        log.debug("Loaded setting: {}", setting);
        return INPUT;
    }

    @Override
    public String doExecute() throws IOException {
        if (this.command == null || this.command.isEmpty()) {
            return this.doDefault();
        }
        log.debug("Saving setting: introspectionEndpoint={},clientId={},clientSecret={},insecureSkipVerify={},trustCaCert={},sessionTimeoutSec={}",
                introspectionEndpoint, clientId, clientSecret, insecureSkipVerify, trustCaCert, sessionTimeoutSec);
        Oauth2BridgeSetting oauth2BridgeSetting = new Oauth2BridgeSetting(introspectionEndpoint, clientId, clientSecret, insecureSkipVerify, trustCaCert, sessionTimeoutSec);
        settingService.updateSetting(oauth2BridgeSetting);
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